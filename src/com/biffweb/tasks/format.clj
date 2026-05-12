(ns com.biffweb.tasks.format
  (:refer-clojure :exclude [format])
  (:require [cljfmt.config :as cljfmt-config]
            [cljfmt.report :as cljfmt-report]
            [cljfmt.tool :as cljfmt]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private clojure-exts #{".clj" ".cljc" ".cljs" ".edn"})

(defn- project-root []
  (io/file (System/getProperty "user.dir")))

(defn- parse-deps []
  (-> (io/file (project-root) "deps.edn")
      slurp
      edn/read-string))

(defn- clojure-file? [path]
  (some #(str/ends-with? path %) clojure-exts))

(defn- existing-paths [paths]
  (->> paths
       (map #(io/file (project-root) %))
       (filter #(.exists %))
       (mapv #(.getPath %))))

(defn- format-paths []
  (let [{:keys [paths aliases]} (parse-deps)
        alias-extra-paths       (->> aliases
                                     vals
                                     (mapcat :extra-paths)
                                     distinct)
        root-files              (->> (.listFiles (project-root))
                                     (filter #(.isFile %))
                                     (map #(.getName %))
                                     (filter clojure-file?))]
    (existing-paths (concat paths alias-extra-paths root-files))))

(defn format
  "Formats the repo's Clojure and EDN files with cljfmt."
  []
  (cljfmt/fix
    (merge
      (cljfmt-config/load-config)
      {:align-form-columns? true
       :align-map-columns?  true
       :extra-aligned-forms {'let #{0}}
       :paths               (format-paths)
       :report              cljfmt-report/clojure}))
  nil)
