(ns com.biffweb.tasks.css
  (:require [clojure.java.io :as io]
            [com.biffweb.tasks.install-tailwind :as install-tailwind]
            [com.biffweb.tasks.util :as util]))

(defn css
  "Generates the target/resources/public/css/main.css file."
  [& tailwind-args]
  (let [{:biff.tasks/keys [css-output tailwind-version]} (util/read-config)
        {:keys [local-bin-installed tailwind-cmd]}       (util/tailwind-installation-info)
        installed-version                                (when (= tailwind-cmd :local-bin)
                                                           (util/local-tailwind-version))]
    (when (and (= tailwind-cmd :local-bin)
               (or (not local-bin-installed)
                   (and tailwind-version
                        local-bin-installed
                        (not= installed-version tailwind-version))))
      (install-tailwind/install-tailwind))
    (when (= tailwind-cmd :local-bin)
      (.setExecutable (io/file (util/local-tailwind-path)) true))
    (try
      (apply util/shell (concat (case tailwind-cmd
                                  :npm ["npx" "tailwindcss"]
                                  :bun ["bunx" "tailwindcss"]
                                  :global-bin [(str (util/which "tailwindcss"))]
                                  :local-bin [(util/local-tailwind-path)])
                                ["-i" "resources/tailwind.css"
                                 "-o" css-output]
                                tailwind-args))
      (catch Exception e
        (if (and (#{137 139} (:exit (ex-data e)))
                 (#{:local-bin :global-bin} tailwind-cmd))
          (binding [*out* *err*]
            (println "It looks like your Tailwind installation is corrupted."
                     "Try deleting it and running this command again:")
            (println)
            (println "  rm" (if (= tailwind-cmd :local-bin)
                              (util/local-tailwind-path)
                              (str (util/which "tailwindcss"))))
            (println))
          (throw e))))))
