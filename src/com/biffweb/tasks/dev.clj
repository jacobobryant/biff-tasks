(ns com.biffweb.tasks.dev 
  (:require
   [clojure.java.io :as io]
   [com.biffweb.cljrun :as cljrun]
   [com.biffweb.tasks.generate :as generate]
   [com.biffweb.tasks.install-tailwind :as install-tailwind]
   [com.biffweb.tasks.util :as util]))

(defn- install-js-deps-cmd []
  (cond
    (util/exists? "bun.lockb") "bun install"
    :else                      "npm install"))

(defn dev
  "Starts the app locally.

   After running, wait for the `System started` message. Connect your editor to
   nrepl port 7888 (by default). Whenever you save a file, Biff will:

   - Evaluate any changed Clojure files
   - Regenerate static HTML and CSS files
   - Run tests"
  []
  (if-not (util/exists? "target/resources")
    ;; This is an awful hack. We have to run the app in a new process, otherwise
    ;; target/resources won't be included in the classpath. Downside of not
    ;; using bb tasks anymore -- no longer have a lightweight parent process
    ;; that can create the directory before starting the JVM.
     (do
       (io/make-parents "target/resources/_")
       (util/shell "clj" "-M:dev" "dev"))
     (let [{:keys [biff.tasks/main-ns biff.nrepl/port]} (util/read-config)]
       (generate/ensure-config-files)
       (when (util/exists? "package.json")
         (util/shell (install-js-deps-cmd)))
       (install-tailwind/ensure-tailwind-installed)
       (util/future (cljrun/run-task "css" "--watch"))
       (spit ".nrepl-port" port)
       ((requiring-resolve (symbol (str main-ns) "-main"))))))
