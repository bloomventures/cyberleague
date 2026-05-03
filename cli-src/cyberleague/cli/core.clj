(ns cyberleague.cli.core
  (:gen-class)
  (:require
   [cli-matic.core :as cli]
   [cyberleague.cli.subcommands.login :as sc.login]
   [cyberleague.cli.subcommands.games :as sc.games]
   [cyberleague.cli.subcommands.envs :as sc.envs]
   [cyberleague.cli.subcommands.bot.new :as sc.bot.new]
   [cyberleague.cli.subcommands.bot.build :as sc.bot.build]
   [cyberleague.cli.subcommands.bot.test :as sc.bot.test]
   [cyberleague.cli.subcommands.bot.upload :as sc.bot.upload]
   [cyberleague.cli.subcommands.bot.deploy :as sc.bot.deploy]
   [cyberleague.cli.subcommands.bot.stage :as sc.bot.stage]))

(def cli-configuration
  {:command "cyberleague"
   :description "cyberleague cli for developing bots"
   :version "2026-04-18"
   :subcommands
   [{:command "login"
     :runs sc.login/exec!}
    {:command "envs"
     :description "List available languages and environments"
     :runs sc.envs/exec!}
    {:command "games"
     :description "List available games"
     :runs sc.games/exec!}
    {:command "bot"
     :subcommands (concat [{:command "new"
                            :description "Create a new bot directory in the current directory. Expects --game and --env."
                            :opts [{:option "game"
                                    :type :string
                                    :default :present
                                    :as "Game; run `cyberleague game` to see available games"}
                                   {:option "env"
                                    :type :string
                                    :default :present
                                    :as "Env; run `cyberleague envs` to see available envs"}]
                            :runs sc.bot.new/exec!}]
                          (->> [{:command "build"
                                 :runs sc.bot.build/exec!
                                 :description "Build an artifact. See bot.edn to adjust build command and artifact location."}
                                {:command "upload"
                                 :runs sc.bot.upload/exec!
                                 :description "Upload a previously built artifact."}
                                {:command "test"
                                 :runs sc.bot.test/exec!
                                 :description "Test a previously uploaded artifact. Returns a URL for viewing results in a browser."}
                                {:command "stage"
                                 :runs sc.bot.stage/exec!
                                 :description "Build, upload, and test a bot artifact."}
                                {:command "deploy"
                                 :runs sc.bot.deploy/exec!
                                 :description "Deploy a previously artifact (ie. enter that version of the bot into competition)"}]
                               (map (fn [c]
                                      (assoc c
                                             :opts [{:option "dir"
                                                     :as "Alternate directory from which to run this command from"
                                                     :type :string
                                                     :default "."}])))))}]})

(defn -main [& args]
  (cli/run-cmd args cli-configuration))

