(ns com.biffweb.tasks.setup
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [com.biffweb.tasks.generate :as generate]
            [com.biffweb.tasks.install-tailwind :as install-tailwind]
            [com.biffweb.tasks.util :as util]))

(def ^:private template-main-ns "com.example")

(defn- project-root []
  (io/file (System/getProperty "user.dir")))

(defn- prompt [msg]
  (print msg)
  (flush)
  (or (not-empty (read-line))
      (recur msg)))

(defn- ns->path [s]
  (-> s
      (str/replace "-" "_")
      (str/replace "." "/")))

(defn- tracked-files []
  (let [{:keys [exit out err]} (sh/sh "git" "ls-files" :dir (.getPath (project-root)))]
    (when-not (zero? exit)
      (throw (ex-info "git ls-files failed" {:exit exit :err err})))
    (->> out
         str/split-lines
         (remove str/blank?))))

(defn- rewrite-main-namespace! [new-main-ns]
  (let [old-path (ns->path template-main-ns)
        new-path (ns->path new-main-ns)
        root (project-root)
        files (map (fn [relative-path]
                     [(io/file root relative-path) relative-path])
                   (tracked-files))]
    (doseq [[file relative-path] files]
      (let [dest-path (str/replace relative-path old-path new-path)
            dest-file (io/file root dest-path)
            contents (slurp file)
            new-contents (str/replace contents template-main-ns new-main-ns)]
        (cond
          (not= relative-path dest-path)
          (do
            (io/make-parents dest-file)
            (spit dest-file new-contents)
            (io/delete-file file))

          (not= contents new-contents)
          (spit file new-contents))))
    (doseq [root ["src" "test"]]
      (let [dir (io/file (project-root) root old-path)]
        (loop [dir dir]
          (when (and (.exists dir)
                     (.isDirectory dir)
                     (empty? (seq (.listFiles dir))))
            (io/delete-file dir)
            (when-let [parent (.getParentFile dir)]
              (recur parent))))))))

(defn- needs-main-namespace-setup? []
  (let [configured-main-ns (some-> (util/read-config)
                                   :biff.tasks/main-ns
                                   str)]
    (or (= configured-main-ns template-main-ns)
        (.exists (io/file (project-root) "src" (str (ns->path template-main-ns) ".clj"))))))

(defn setup
  "Initializes a freshly cloned Biff project."
  [& [main-ns]]
  (when (needs-main-namespace-setup?)
    (let [main-ns (str (or main-ns
                           (prompt "Enter main namespace (e.g. com.example): ")))]
      (rewrite-main-namespace! main-ns)
      (println "Updated the main namespace to" main-ns ".")))
  (generate/ensure-config-files)
  (install-tailwind/ensure-tailwind-installed))
