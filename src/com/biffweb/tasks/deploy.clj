(ns com.biffweb.tasks.deploy
  (:require [clojure.string :as str]
            [com.biffweb.cljrun :as cljrun]
            [com.biffweb.tasks.util :as util]))

(defn- git-push-url [ctx]
  (str "ssh://" (util/ssh-target ctx) (util/remote-repo-path ctx)))

(defn- remote-branch-head [ctx branch]
  (let [repo          (util/remote-repo-path ctx)
        {:keys [out]} (util/ssh-capture-shell
                       ctx
                       (str "if [ -d " (util/shell-quote (str repo "/.git")) " ]; then "
                            "git -C " (util/shell-quote repo)
                            " rev-parse --verify refs/heads/" (util/shell-quote branch)
                            " 2>/dev/null || true; fi"))]
    (some-> out str/trim not-empty)))

(defn- ensure-remote-repo! [ctx branch]
  (let [repo (util/remote-repo-path ctx)]
    (util/ssh-run-shell
     ctx
     (str "mkdir -p " (util/shell-quote repo) " && "
          "if [ ! -d " (util/shell-quote (str repo "/.git")) " ]; then "
          "git init " (util/shell-quote repo) " && "
          "git -C " (util/shell-quote repo) " checkout -B " (util/shell-quote branch) "; "
          "fi && "
          "git -C " (util/shell-quote repo) " config receive.denyCurrentBranch updateInstead && "
          "if ! git -C " (util/shell-quote repo) " symbolic-ref HEAD >/dev/null 2>&1; then "
          "git -C " (util/shell-quote repo) " checkout -B " (util/shell-quote branch) "; "
          "fi"))))

(defn- push-branch! [ctx branch]
  (util/shell "git" "push" "--force" (git-push-url ctx) (str "HEAD:" branch)))

(defn- checkout-remote-branch! [ctx branch]
  (util/ssh-run-shell
   ctx
   (str "git -C " (util/shell-quote (util/remote-repo-path ctx))
        " checkout -f " (util/shell-quote branch))))

(defn- changed-clojure-files [ctx old-sha]
  (when old-sha
    (let [repo          (util/remote-repo-path ctx)
          {:keys [out]} (util/ssh-capture-shell
                         ctx
                         (str "git -C " (util/shell-quote repo)
                              " diff --name-status " old-sha " HEAD -- '*.clj' '*.cljc'"))]
      (reduce (fn [{:keys [changed deleted] :as acc} line]
                (let [[status path] (str/split line #"\t" 2)]
                  (cond
                    (str/blank? line) acc
                    (= status "D") (update acc :deleted conj path)
                    :else (update acc :changed conj path))))
              {:changed [] :deleted []}
              (str/split-lines out)))))

(defn- soft-deploy! [ctx old-sha]
  (let [{:biff.tasks/keys [nrepl-port]} ctx]
    (when-not old-sha
      (throw (ex-info "--soft requires an existing deployed branch on the server." {})))
    (when-not nrepl-port
      (throw (ex-info ":biff.tasks/nrepl-port must be set for deploy --soft." {})))
    (let [{:keys [changed deleted]} (changed-clojure-files ctx old-sha)]
      (when (seq deleted)
        (throw (ex-info "--soft cannot handle deleted Clojure files; run deploy without --soft."
                        {:deleted deleted})))
      (when (seq changed)
        (util/ssh-run
         ctx
         "trench"
         "-p"
         (str nrepl-port)
         "-e"
         (str "(do (require 'com.biffweb.tasks.dev) "
              "(com.biffweb.tasks.dev/eval-changed-files! "
              (pr-str changed)
              "))"))))))

(defn deploy
  "Pushes code to the server and either restarts it or performs a soft reload."
  [& args]
  (let [soft?   (some #{"--soft"} args)
        ctx     (util/read-config)
        branch  (util/current-git-branch)
        old-sha (remote-branch-head ctx branch)]
    (util/ensure-clean-worktree!)
    (util/with-ssh-agent ctx
      (cljrun/run-task "css" "--minify")
      (ensure-remote-repo! ctx branch)
      (push-branch! ctx branch)
      (checkout-remote-branch! ctx branch)
      (util/push-deploy-files! ctx)
      (if soft?
        (soft-deploy! ctx old-sha)
        (cljrun/run-task "prod-restart")))))
