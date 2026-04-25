(ns com.biffweb.tasks.test
  (:refer-clojure :exclude [test])
  (:require [clojure.java.process :as process]))

(defn test
  "Runs project tests from the test/ path."
  []
  (let [proc (process/start {:in :inherit
                             :out :inherit
                             :err :inherit}
                            "java"
                            "-cp" (System/getProperty "java.class.path")
                            "clojure.main"
                            "-e"
                            "(let [{:keys [fail error]} ((requiring-resolve 'cognitect.test-runner.api/test) {:dirs [\"test\"]})]\n  (shutdown-agents)\n  (System/exit (if (zero? (+ fail error)) 0 1)))")
        exit-code (.waitFor proc)]
    (when-not (zero? exit-code)
      (throw (ex-info "Tests failed" {:exit exit-code})))))
