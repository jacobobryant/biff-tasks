(ns com.biffweb.tasks.format
  (:refer-clojure :exclude [format])
  (:require [cljfmt.config :as cljfmt-config]
            [cljfmt.report :as cljfmt-report]
            [cljfmt.tool :as cljfmt]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.biffweb.tasks.util :as util]))

(def ^:private clojure-exts #{".clj" ".cljc" ".cljs" ".edn"})

(defn- project-root []
  (io/file (System/getProperty "user.dir")))

(defn- clojure-file? [path]
  (some #(str/ends-with? path %) clojure-exts))

(defn- format-paths []
  (let [root-files (->> (.listFiles (project-root))
                        (filter #(.isFile %))
                        (map #(.getName %))
                        (filter clojure-file?))]
    (->> (concat (util/deps-paths) root-files)
         distinct
         (mapv #(.getPath (io/file (project-root) %))))))

(defn format
  "Formats the repo's Clojure and EDN files with cljfmt."
  []
  (cljfmt/fix
   (merge
    {:align-form-columns? true
     :align-map-columns?  true
     :extra-aligned-forms {'let #{0}}}
    (cljfmt-config/load-config)
    {:paths  (format-paths)
     :report cljfmt-report/clojure}))
  nil)
