(ns cyberleague.coordinator.test-bot
  (:require
    [clojure.java.io :as io]
    [cyberleague.db.core :as db]
    [cyberleague.coordinator.game-runner :as game-runner]))

(defn test-bot
  [user-id bot-id bot]
  ;; TODO: use an appropriate random bot for different games
  (let [game-name (get-in bot [:bot/game :game/name])
        random-bot-id 1234
        random-bot {:db/id random-bot-id
                    :bot/code-version 5
                    :bot/code {:code/code (slurp (io/resource (str "testbots/" game-name ".cljs")))
                               :code/language "clojure"}}
        coded-bot (-> (into {} bot)
                      (assoc :db/id (:db/id bot)
                             :bot/code-version (rand-int 10000000)
                             :bot/code (:bot/code bot)))
        result (game-runner/run-game (into {} (:bot/game bot))
                                     [coded-bot random-bot])
        match {:game {:name game-name}
               :bots [{:id (:db/id bot)
                       :name "You"}
                      {:id random-bot-id
                       :name "Them"}]
               :moves (result :history)
               :winner (result :winner)
               :error (result :error)
               :info (result :info)}]
    {:body match}))
