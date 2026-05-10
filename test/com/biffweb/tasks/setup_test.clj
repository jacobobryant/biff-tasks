(ns com.biffweb.tasks.setup-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.biffweb.tasks.install-tailwind :as install-tailwind]
            [com.biffweb.tasks.setup :as setup]
            [com.biffweb.tasks.util :as util]))

(defn- rmrf [file]
  (when (.exists file)
    (when (.isDirectory file)
      (run! rmrf (.listFiles file)))
    (io/delete-file file)))

(defmacro with-temp-dir [[sym] & body]
  `(let [~sym (.toFile (java.nio.file.Files/createTempDirectory "biff-tasks-setup-test"
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

(defn- slurp-file [dir relative-path]
  (slurp (io/file dir relative-path)))

(defn- sh! [dir & args]
  (let [{:keys [exit err]} (apply sh/sh (concat args [:dir (.getPath dir)]))]
    (when-not (zero? exit)
      (throw (ex-info "Command failed" {:args args :err err :dir (.getPath dir)})))))

(defn- init-git! [dir]
  (sh! dir "git" "init")
  (sh! dir "git" "config" "user.name" "Copilot")
  (sh! dir "git" "config" "user.email" "copilot@example.com")
  (sh! dir "git" "add" "."))

(deftest setup-renames-template-namespace-and-creates-missing-files
  (with-temp-dir [dir]
    (write-file dir "deps.edn" "{:aliases {:prod {:main-opts [\"-m\" \"com.acme-app\"]}}}\n")
    (write-file dir "README.md" "Use com.acme-app here.\n")
    (write-file dir "resources/config.edn" "{:biff.tasks/main-ns com.acme-app}\n")
    (write-file dir "resources/TEMPLATE.config.env" "COOKIE_SECRET={{ new-secret 4 }}\n")
    (write-file dir "resources/TEMPLATE.config.prod.env" "COOKIE_SECRET={{ new-secret 4 }}\n")
    (write-file dir "src/com/example.clj" "(ns com.acme-app)\n")
    (write-file dir "src/com/example/lib/foo.clj" "(ns com.acme-app.lib.foo)\n")
    (write-file dir "test/com/example/foo_test.clj" "(ns com.acme-app.foo-test)\n")
    (init-git! dir)
    (let [tailwind-called? (atom false)]
      (with-user-dir dir
        (with-redefs [util/read-config (constantly {:biff.tasks/main-ns 'com.acme-app})
                      install-tailwind/ensure-tailwind-installed #(reset! tailwind-called? true)]
          (setup/setup "com.acme-app")))
      (is @tailwind-called?)
      (is (not (.exists (io/file dir "src/com/example.clj"))))
      (is (= "(ns com.acme-app)\n"
             (slurp-file dir "src/com/acme_app.clj")))
      (is (= "(ns com.acme-app.lib.foo)\n"
             (slurp-file dir "src/com/acme_app/lib/foo.clj")))
      (is (= "(ns com.acme-app.foo-test)\n"
             (slurp-file dir "test/com/acme_app/foo_test.clj")))
      (is (str/includes? (slurp-file dir "resources/config.edn")
                         ":biff.tasks/main-ns com.acme-app"))
      (is (str/includes? (slurp-file dir "deps.edn")
                         "\"com.acme-app\""))
      (is (str/includes? (slurp-file dir "README.md")
                         "com.acme-app"))
      (is (re-find #"COOKIE_SECRET=\S+" (slurp-file dir "config.env")))
      (is (re-find #"COOKIE_SECRET=\S+" (slurp-file dir "config.prod.env"))))))

(deftest setup-skips-namespace-rewrite-on-subsequent-runs
  (with-temp-dir [dir]
    (write-file dir "resources/TEMPLATE.config.env" "COOKIE_SECRET={{ new-secret 4 }}\n")
    (write-file dir "resources/TEMPLATE.config.prod.env" "COOKIE_SECRET={{ new-secret 4 }}\n")
    (write-file dir "config.env" "existing=true\n")
    (init-git! dir)
    (let [tailwind-called? (atom false)]
      (with-user-dir dir
        (with-redefs [util/read-config (constantly {:biff.tasks/main-ns 'com.acme-app})
                      install-tailwind/ensure-tailwind-installed #(reset! tailwind-called? true)]
          (setup/setup)))
      (is @tailwind-called?)
      (is (= "existing=true\n" (slurp-file dir "config.env")))
      (is (re-find #"COOKIE_SECRET=\S+" (slurp-file dir "config.prod.env")))
      (is (not (.exists (io/file dir "src/com/example.clj")))))))
