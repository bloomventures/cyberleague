(ns cyberleague.cli.subcommands.bot.upload
  (:require
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.bot-dev :as bot-dev]))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (bot-dev/upload! bot-config)))
