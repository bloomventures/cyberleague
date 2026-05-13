(ns cyberleague.cli.subcommands.bot.undeploy
  (:require
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (println (f/color :color/yellow "Undeploying..."))
    (r/tada! [:api/undeploy-bot!
              {:bot-id (:bot/id bot-config)}])
    (println "Undeploy successful.")))
