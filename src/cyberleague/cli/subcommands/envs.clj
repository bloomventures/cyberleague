(ns cyberleague.cli.subcommands.envs
  (:require
   [cyberleague.cli.util.remote :as r]))

(defn exec!
  [_]
  (let [languages (r/tada! [:api/languages {}])]
    (doseq [language (->> languages
                          (sort-by :language/slug))]
      (println (str (:language/slug language) ":"))
      (doseq [env (:language/envs language)]
        (println "  " (:env/slug env))))))
