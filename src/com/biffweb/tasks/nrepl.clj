(ns com.biffweb.tasks.nrepl
  (:require [com.biffweb.tasks.util :as util]
            [nrepl.server :as nrepl]))

(defn nrepl
  "Starts an nREPL server without starting the application."
  []
  (let [{:biff.tasks/keys [nrepl-port]} (util/read-config)
        server                          (if nrepl-port
                                          (nrepl/start-server :port nrepl-port)
                                          (nrepl/start-server))
        port                            (:port server)]
    (spit ".nrepl-port" port)
    (println "nREPL server started on port" port)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(nrepl/stop-server server)))
    @(promise)))
