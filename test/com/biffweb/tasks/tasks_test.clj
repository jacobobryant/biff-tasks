(ns com.biffweb.tasks.tasks-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [com.biffweb.tasks :as tasks]
            [com.biffweb.tasks.css :as css]
            [com.biffweb.tasks.format :as tasks-format]
            [com.biffweb.tasks.install-tailwind :as install-tailwind]
            [com.biffweb.tasks.util :as util]))

(defn- rmrf [file]
  (when (.exists file)
    (when (.isDirectory file)
      (run! rmrf (.listFiles file)))
    (io/delete-file file)))

(defmacro with-temp-dir [[sym] & body]
  `(let [~sym (.toFile (java.nio.file.Files/createTempDirectory "biff-tasks-task-test"
                                                                (make-array java.nio.file.attribute.FileAttribute 0)))]
     (try
       ~@body
       (finally
         (rmrf ~sym)))))

(defmacro with-user-dir [dir & body]
  `(let [original# (System/getProperty "user.dir")]
     (try
       (System/setProperty "user.dir" (.getPath ~dir))
       ~@body
       (finally
         (System/setProperty "user.dir" original#)))))

(defn- write-file [dir relative-path contents]
  (let [file (io/file dir relative-path)]
    (io/make-parents file)
    (spit file contents)))

(deftest task-surface-matches-spec
  (is (= #{"css"
           "deploy"
           "dev"
           "format"
           "nrepl"
           "prod-install"
           "prod-logs"
           "prod-nrepl"
           "prod-restart"
           "setup"
           "test"
           "uberjar"}
         (set (keys tasks/tasks)))))

(deftest css-redownloads-local-binary-when-version-is-pinned-and-mismatched
  (let [installs (atom 0)
        commands (atom [])]
    (with-redefs [util/read-config                  (constantly {:biff.tasks/css-output       "target/resources/public/css/main.css"
                                                                 :biff.tasks/tailwind-version "4.1.14"})
                  util/tailwind-installation-info   (constantly {:local-bin-installed true
                                                                 :tailwind-cmd        :local-bin})
                  util/local-tailwind-version       (constantly "4.1.13")
                  util/local-tailwind-path          (constantly "bin/tailwindcss")
                  install-tailwind/install-tailwind #(swap! installs inc)
                  util/shell                        (fn [& args]
                                                      (swap! commands conj (vec args)))]
      (css/css "--minify"))
    (is (= 1 @installs))
    (is (= [["bin/tailwindcss"
             "-i"
             "resources/tailwind.css"
             "-o"
             "target/resources/public/css/main.css"
             "--minify"]]
           @commands))))

(deftest css-ignores-pinned-version-outside-local-binary-mode
  (let [installs (atom 0)]
    (with-redefs [util/read-config                  (constantly {:biff.tasks/css-output       "target/resources/public/css/main.css"
                                                                 :biff.tasks/tailwind-version "4.1.14"})
                  util/tailwind-installation-info   (constantly {:local-bin-installed false
                                                                 :tailwind-cmd        :npm})
                  install-tailwind/install-tailwind #(swap! installs inc)
                  util/shell                        (fn [& _args])]
      (css/css "--watch"))
    (is (zero? @installs))))

(deftest format-uses-deps-paths-and-root-edn-files
  (with-temp-dir [dir]
    (write-file dir "deps.edn" "{:paths [\"src\"], :aliases {:test {:extra-paths [\"test\"]}}}\n")
    (write-file dir "src/com/example.clj" "(defn add [x y]\n(+ x y))\n")
    (write-file dir "test/com/example_test.clj" "(ns com.example-test)\n")
    (write-file dir "config.edn" "{:foo 1\n :bar 2}\n")
    (with-user-dir dir
      (tasks-format/format))
    (is (= "(defn add [x y]\n  (+ x y))\n"
           (slurp (io/file dir "src/com/example.clj"))))))
