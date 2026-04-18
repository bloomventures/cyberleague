(ns cyberleague.cli.subcommands.bot.build
  (:require
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.bot-dev :as bot-dev]))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (bot-dev/build! bot-config)))

