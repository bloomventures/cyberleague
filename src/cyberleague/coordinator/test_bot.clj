(ns cyberleague.coordinator.test-bot
  (:require
   [clojure.java.io :as io]
   [cyberleague.game-registrar :as registrar]
   [cyberleague.coordinator.game-runner :as game-runner]))

(defn test-bot
  [user-id bot-id bot]
  ;; TODO: use an appropriate random bot for different games
  (let [game-name (get-in bot [:bot/game :game/name])
        random-bot-id 1234
        random-bot {:db/id random-bot-id
                    :bot/code-version 5
                    :bot/code {:code/code (get-in @registrar/games [game-name :game.config/test-bot])
                               :code/language "clojure"}}
        coded-bot (-> (into {} bot)
                      (assoc :db/id (:db/id bot)
                             :bot/code-version (rand-int 10000000)
                             :bot/code (:bot/code bot)))
        result (game-runner/run-game (into {} (:bot/game bot))

                                     [coded-bot random-bot])]
    (when result
      {:match/game {:game/name game-name}
       :match/state-history (result :game.result/state-history)
       :match/bots [{:bot/id (:db/id bot)
                     :bot/name "You"}
                    {:bot/id random-bot-id
                     :bot/name "Them"}]
       :match/moves (result :game.result/history)
       :winner (result :game.result/winner)
       :match/error (result :game.result/error)
       :match/info (result :info)})))
