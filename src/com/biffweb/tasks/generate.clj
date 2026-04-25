(ns com.biffweb.tasks.generate
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.biffweb.tasks.util :as util]))

(defn- new-secret [length]
  (let [buffer (byte-array length)]
    (.nextBytes (java.security.SecureRandom/getInstanceStrong) buffer)
    (.encodeToString (java.util.Base64/getEncoder) buffer)))

(defn generate-secrets
  "Prints new secrets to put in config.env."
  []
  (println "Put these in your config.env file:")
  (println)
  (println (str "COOKIE_SECRET=" (new-secret 16)))
  (println (str "JWT_SECRET=" (new-secret 32)))
  (println))

(defn- render-config-template [resource-name]
  (-> (slurp (io/resource resource-name))
       (str/replace #"\{\{\s+new-secret\s+(\d+)\s+\}\}"
                    (fn [[_ n]]
                      (new-secret (parse-long n))))))

(defn generate-config
  "Creates new config.env and config.prod.env files if they don't already exist."
  []
  (if (or (util/exists? "config.env")
          (util/exists? "config.prod.env"))
    (binding [*out* *err*]
      (println "config.env or config.prod.env already exists. If you want to generate new files, move them out of the way first.")
      (System/exit 3))
    (let [dev-contents (render-config-template "TEMPLATE.config.env")
          prod-contents (render-config-template "TEMPLATE.config.prod.env")]
      (spit "config.env" dev-contents)
      (spit "config.prod.env" prod-contents)
      (println "New config generated and written to config.env and config.prod.env."))))
