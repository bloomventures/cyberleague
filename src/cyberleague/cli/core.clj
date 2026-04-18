(ns cyberleague.cli.core
  (:require
   [cli-matic.core :as cli]
   [cyberleague.cli.subcommands.login :as sc.login]
   [cyberleague.cli.subcommands.games :as sc.games]
   [cyberleague.cli.subcommands.envs :as sc.envs]
   [cyberleague.cli.subcommands.bot.new :as sc.bot.new]
   [cyberleague.cli.subcommands.bot.watch :as sc.bot.watch]))

(def cli-configuration
  {:command "cyberleague"
   :description "cyberleague cli for developing bots"
   :version "2026-04-18"
   :subcommands
   [{:command "login"
     :runs sc.login/login!}
    {:command "envs"
     :description "List available languages and environments"
     :runs sc.envs/list-envs!}
    {:command "games"
     :description "List available games"
     :runs sc.games/list-games!}
    #_{:command "bots"}
    {:command "bot"
     :subcommands [{:command "new"
                    :opts [{:option "game"
                            :type :string
                            :default :present
                            :as "Game; run `cyberleague game` to see available games"}
                           {:option "env"
                            :type :string
                            :default :present
                            :as "Env; run `cyberleague envs` to see available envs"}]
                    :runs sc.bot.new/create-bot!}
                   #_{:command "deploy"}
                   #_{:command "fetch"}
                   #_{:command "test"}
                   #_{:command "watch"
                      :runs (watch! (:watch options))}]}]})

(defn -main [& args]
  (cli/run-cmd args cli-configuration))

