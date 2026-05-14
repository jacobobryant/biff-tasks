(ns com.biffweb.tasks.dev
  (:require [clojure.java.io :as io]
            [clojure.stacktrace :as st]
            [com.biffweb.cljrun :as cljrun]
            [com.biffweb.tasks.generate :as generate]
            [com.biffweb.tasks.install-tailwind :as install-tailwind]
            [com.biffweb.tasks.reload :as reload]
            [com.biffweb.tasks.test :as tasks-test]
            [com.biffweb.tasks.util :as util]
            [nextjournal.beholder :as beholder])
  (:import [java.util Timer TimerTask]))

(def ^:private status-file ".biff-dev-status.edn")

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

(defn- throwable->data [t]
  {:class      (str (class t))
   :message    (.getMessage t)
   :ex-data    (ex-data t)
   :stacktrace (with-out-str (st/print-stack-trace t))})

(defn eval-changed-files!
  "Evaluates changed local Clojure source files in the current JVM."
  []
  (let [result (reload/refresh! (util/deps-paths))]
    (when (instance? Throwable result)
      (throw result))
    result))

(defn- process-changes! []
  (write-status! {:status :running})
  (try
    (eval-changed-files!)
    (let [{:keys [fail error] :as result} (merge {:fail 0 :error 0}
                                                 (tasks-test/run-tests))]
      (if (zero? (+ fail error))
        (write-status! {:status :ok})
        (write-status! {:status       :test-failure
                        :test-failure result})))
    (catch Throwable t
      (binding [*out* *err*]
        (st/print-stack-trace t))
      (write-status! {:status       :eval-failure
                      :eval-failure (throwable->data t)}))))

(defn- start-watchers! []
  (let [flush! (debounce process-changes! 500)]
    (apply beholder/watch
           (fn [_event]
             (flush!))
           (util/deps-paths))))

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
