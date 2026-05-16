(ns com.biffweb.tasks.test
  (:refer-clojure :exclude [test])
  (:require [cognitect.test-runner.api :as test-runner]))

(defn run-tests []
  (or (test-runner/test {:dirs ["test"]})
      {:fail 0 :error 0}))

(defn test
  "Runs project tests from the test/ path."
  []
  (let [{:keys [fail error] :as result} (merge {:fail 0 :error 0} (run-tests))]
    (when-not (zero? (+ fail error))
      (throw (ex-info "Tests failed" result)))))
