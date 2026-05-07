(ns cyberleague.cli.subcommands.envs
  (:require
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]))

(defn exec!
  [_]
  (when-let [languages (r/tada! [:api/languages {}])]
    (println (f/color :color/yellow "Envs:"))
    (doseq [language (->> languages
                          (sort-by :language/slug))]
      (println " " (str (:language/slug language) ":"))
      (doseq [env (:language/envs language)]
        (println "   " (str (:env/slug env)
                            (when-let [status (:env/status env)]
                              (str " (" status ")"))))))))
