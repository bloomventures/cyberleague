(ns cyberleague.cli.subcommands.bot.test-remote
  (:require
   [cyberleague.common.artifact :as artifact]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]))

(defn test-remote!
  [bot-config]
  (println (f/color :color/yellow "Testing..."))
  (let [a (bot-config/->artifact bot-config)]
    (println "Digest:" (artifact/digest a))
    (if-let [_match-id (:match/id (r/tada! ^{:timeout 120000}
                                            [:api/test-bot!
                                             {:bot-id (:bot/id bot-config)
                                              :digest (artifact/digest a)}]))]
      (println "Test completed. View it online.")
      (println "Error running test."))))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (test-remote! bot-config)))
