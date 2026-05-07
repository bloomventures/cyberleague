(ns cyberleague.cli.subcommands.bot.stage
  (:require
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.subcommands.bot.build :as build]
   [cyberleague.cli.subcommands.bot.test-local :as test-local]
   [cyberleague.cli.subcommands.bot.upload :as upload]
   [cyberleague.cli.subcommands.bot.test-remote :as test-remote]))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (build/build! bot-config)
    (test-local/test-local! bot-config)
    (upload/upload! bot-config)
    (test-remote/test-remote! bot-config)))
