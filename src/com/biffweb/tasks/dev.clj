(ns com.biffweb.tasks.dev
  (:require [clojure.java.io :as io]
            [clojure.stacktrace :as st]
            [clojure.string :as str]
            [com.biffweb.cljrun :as cljrun]
            [com.biffweb.tasks.generate :as generate]
            [com.biffweb.tasks.install-tailwind :as install-tailwind]
            [com.biffweb.tasks.test :as tasks-test]
            [com.biffweb.tasks.util :as util]
            [nextjournal.beholder :as beholder])
  (:import [java.util Timer TimerTask]))

(def ^:private status-file ".biff-dev-status.edn")
(def ^:private watched-dirs ["src" "dev" "resources" "test"])
(def ^:private eval-exts #{".clj" ".cljc"})
(def ^:private test-exts #{".clj" ".cljc" ".cljs" ".edn"})

;; https://gist.github.com/oliyh/0c1da9beab43766ae2a6abc9507e732a
(defn- debounce
  ([f] (debounce f 1000))
  ([f timeout]
   (let [timer (Timer.)
         task  (atom nil)]
     (with-meta
       (fn [& args]
         (when-let [t ^TimerTask @task]
           (.cancel t))
         (let [new-task (proxy [TimerTask] []
                          (run []
                            (apply f args)
                            (reset! task nil)
                            (.purge timer)))]
           (reset! task new-task)
           (.schedule timer new-task timeout)))
       {:task-atom task}))))

(defn- install-js-deps-cmd []
  (if (util/exists? "bun.lockb")
    "bun install"
    "npm install"))

(defn- now []
  (str (java.time.Instant/now)))

(defn- write-status! [m]
  (spit status-file (str (pr-str (assoc m :timestamp (now))) "\n")))

(defn- ext [path]
  (some #(when (str/ends-with? path %) %) test-exts))

(defn- relative-path [path]
  (let [root (.toPath (io/file (System/getProperty "user.dir")))
        path (.normalize (.toPath (io/file (str path))))]
    (if (.startsWith path root)
      (str (.relativize root path))
      (str path))))

(defn- eval-path? [path]
  (and (eval-exts (ext path))
       (or (str/starts-with? path "src/")
           (str/starts-with? path "dev/"))))

(defn- relevant-test-path? [path]
  (and (test-exts (ext path))
       (some #(str/starts-with? path (str % "/")) watched-dirs)))

(defn- throwable->data [t]
  {:class      (str (class t))
   :message    (.getMessage t)
   :ex-data    (ex-data t)
   :stacktrace (with-out-str (st/print-stack-trace t))})

(defn eval-changed-files!
  "Evaluates changed local Clojure source files in the current JVM."
  [paths]
  (let [paths (->> paths distinct sort vec)]
    (doseq [path paths]
      (when-not (util/exists? path)
        (throw (ex-info "Deleted Clojure files require a manual restart."
                        {:path path})))
      (load-file path))
    {:files paths}))

(defn- process-changes! [paths]
  (let [paths      (->> paths distinct sort vec)
        eval-paths (filterv eval-path? paths)
        run-tests? (boolean (some relevant-test-path? paths))]
    (when (seq paths)
      (write-status! {:status :running})
      (try
        (when (seq eval-paths)
          (eval-changed-files! eval-paths))
        (if run-tests?
          (let [{:keys [fail error] :as result} (merge {:fail 0 :error 0}
                                                       (tasks-test/run-tests))]
            (if (zero? (+ fail error))
              (write-status! {:status :ok})
              (write-status! {:status       :test-failure
                              :test-failure result})))
          (write-status! {:status :ok}))
        (catch Throwable t
          (binding [*out* *err*]
            (st/print-stack-trace t))
          (write-status! {:status       :eval-failure
                          :eval-failure (throwable->data t)}))))))

(defn- start-watchers! []
  (let [pending-events (atom [])
        flush!         (debounce
                        (fn []
                          (let [events @pending-events
                                _      (reset! pending-events [])
                                paths  (->> events
                                            (map (comp relative-path :path))
                                            (filter not-empty))]
                            (process-changes! paths)))
                        500)]
    (apply beholder/watch
           (fn [event]
             (swap! pending-events conj event)
             (flush!))
           watched-dirs)))

(defn dev
  "Starts the app locally and keeps CSS/tests/file evaluation up to date."
  [& args]
  (let [minify-css? (some #{"--minify-css"} args)]
    (if-not (util/exists? "target/resources")
      (do
        (io/make-parents "target/resources/_")
        (apply util/shell (concat ["clj" "-M:run" "dev"]
                                  (when minify-css?
                                    ["--minify-css"]))))
      (let [{:biff.tasks/keys [main-ns nrepl-port]} (util/read-config)]
        (generate/ensure-config-files)
        (when (util/exists? "package.json")
          (util/shell (install-js-deps-cmd)))
        (install-tailwind/ensure-tailwind-installed)
        (util/future
          (apply cljrun/run-task
                 (concat ["css" "--watch"]
                         (when minify-css?
                           ["--minify"]))))
        (start-watchers!)
        (when nrepl-port
          (spit ".nrepl-port" nrepl-port))
        ((requiring-resolve (symbol (str main-ns) "-main")))))))
