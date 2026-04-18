(ns cyberleague.coordinator.test-bot
  (:require
   [clojure.java.io :as io]
   [bloom.commons.uuid :as uuid]
   [cyberleague.coordinator.game-runner :as game-runner]
   [cyberleague.game-registrar :as registrar]))

(defn test-bot
  [user-id bot-id bot]
  (let [game-slug (get-in bot [:bot/game :game/slug])
        random-bot-id 1234
        random-bot {:bot/id random-bot-id
                    :bot/code-version 5
                    :bot/code {:code/code (get-in @registrar/games [game-slug :game.config/test-bot])
                               :code/env {:env/slug "clojure-sci"}}}
        coded-bot (-> (into {} bot)
                      (assoc
                       :bot/code-version (rand-int 10000000)
                       :bot/code (:bot/code bot)))
        result (game-runner/run-game (into {} (:bot/game bot))
                                     [coded-bot random-bot])]
    (when result
      ;; mimicking a match so that we can reuse the same views on the frontend
      {:match/id (uuid/random)
       :match/game {:game/slug game-slug}
       :match/state-history (:game.result/state-history result)
       :match/std-out-history (:game.result/std-out-history result)
       :match/bots [{:bot/id (:bot/id bot)
                     :bot/name "You"}
                    {:bot/id random-bot-id
                     :bot/name "Them"}]
       :match/moves (:game.result/history result)
       :winner (:game.result/winner result)
       :match/error (:game.result/error result)})))

