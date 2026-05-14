(ns com.biffweb.tasks.reload
  (:require [clojure.repl :as repl]
            [clojure.string :as str]
            [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.reload :as reload]
            clojure.tools.namespace.repl
            [clojure.tools.namespace.track :as track]))

(defonce global-tracker (atom (track/tracker)))

(def remove-disabled #'clojure.tools.namespace.repl/remove-disabled)

(defn- print-pending-reloads [tracker]
  (when-let [reloads (seq (::track/load tracker))]
    (prn :reloading reloads)))

(defn- print-and-return [tracker]
  (if-let [e (::reload/error tracker)]
    (do
      (when (thread-bound? #'*e)
        (set! *e e))
      (prn :error-while-loading (::reload/error-ns tracker))
      (repl/pst e)
      e)
    :ok))

(defn refresh!
  "Reloads changed namespaces using the same tracker-driven tools.namespace flow
  Biff used previously."
  [directories]
  (let [directories  (filterv (set (str/split (System/getProperty "java.class.path") #":"))
                              directories)
        new-tracker  (dir/scan-dirs @global-tracker directories)
        new-tracker  (remove-disabled new-tracker)
        _            (print-pending-reloads new-tracker)
        new-tracker  (reload/track-reload (assoc new-tracker ::track/unload []))
        refresh-exit (print-and-return new-tracker)]
    (reset! global-tracker new-tracker)
    refresh-exit))
