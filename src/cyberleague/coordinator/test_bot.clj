(ns cyberleague.coordinator.test-bot
  (:require
   [cyberleague.db.core :as db]
   [cyberleague.coordinator.core :as coordinator]))

(defn test-bot
  [{:keys [bot-id artifact-id]}]
  (let [bot (db/by-id [:bot/id bot-id])
        game-slug (get-in bot [:bot/game :game/slug])

        [dummy-bot-eid dummy-artifact-eid] (db/dummy-bot game-slug)]
    (coordinator/run-game!
     {:game (into {} (:bot/game bot))
      :bots [bot
             (db/by-id dummy-bot-eid)]
      :artifacts [(db/by-id [:artifact/id artifact-id])
                  (db/by-id dummy-artifact-eid)]
      :test? true})))


