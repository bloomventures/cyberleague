(ns cyberleague.cli.subcommands.bot.deploy
  (:require
   [clojure.string]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]))

(defn deploy!
  [bot-config digest]
  (println (f/color :color/yellow "Deploying..."))
  (r/tada! [:api/deploy-bot!
            {:bot-id (:bot/id bot-config)
             :digest digest}])
  (println "Deploy successful."))

(defn exec!
  [{:keys [dir digest]}]
  (when-let [bot-config (bot-config/read! dir)]
    (let [resolved-digest (if (not (clojure.string/blank? digest))
                            digest
                            (artifact/digest (bot-config/->artifact bot-config)))]
      (deploy! bot-config resolved-digest))))
