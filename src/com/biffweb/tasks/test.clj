(ns com.biffweb.tasks.test
  (:refer-clojure :exclude [test]))

(defn test
  "Runs project tests from the test/ path."
  []
  ((requiring-resolve 'cognitect.test-runner.api/test) {:dirs ["test"]}))
