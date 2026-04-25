(ns com.biffweb.tasks.ssh
  (:require [com.biffweb.tasks.util :as util]))

(defn logs
  "Tails the server's application logs."
  ([]
   (logs "300"))
  ([n-lines]
   (let [{:biff.tasks/keys [prod-dir]
          :or {prod-dir "app"}} (util/read-config)]
     (util/ssh-run (util/read-config) "journalctl" "-u" prod-dir "-f" "-n" n-lines))))

(defn restart
  "Restarts the app process via `systemctl restart <prod-dir>` (on the server)."
  []
  (let [{:biff.tasks/keys [prod-dir]
         :or {prod-dir "app"}} (util/read-config)]
    (util/ssh-run (util/read-config)
                  (str "sudo systemctl reset-failed " prod-dir ".service; "
                       "sudo systemctl restart " prod-dir))))

(defn prod-repl
  "Opens an SSH tunnel so you can connect to the server via nREPL."
  []
  (let [{:keys [biff.tasks/server biff.nrepl/port prod-dir]
         :or {prod-dir "app"}} (util/read-config)]
    (println "Connect to nrepl port" port)
    (spit ".nrepl-port" port)
    (util/shell "ssh" "-NL" (str port ":localhost:" port) (str prod-dir "@" server))))
