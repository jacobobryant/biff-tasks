(ns com.biffweb.tasks.test
  (:require [clojure.java.shell :as sh]))

(defn test
  "Runs `clojure.test` via Cognitect's test runner."
  []
  (System/exit
   (:exit (sh/sh "clojure" "-X:test"))))
