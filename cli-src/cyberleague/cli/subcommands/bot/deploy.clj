(ns cyberleague.cli.subcommands.bot.deploy
  (:require
   [cyberleague.common.artifact :as artifact]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]))

(defn deploy!
  [bot-config]
  (println (f/color :color/yellow "Deploying..."))
  (let [a (bot-config/->artifact bot-config)]
    (r/tada! [:api/deploy-bot!
              {:bot-id (:bot/id bot-config)
               :digest (artifact/digest a)}])
    (println "Deploy successful.")))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (deploy! bot-config)))
