(ns pog.core
  (:require [pog.db :as db]
            [pog.ranking :as ranking]
            [pog.game-runner :as game-runner]))

(defn -main
  [& args]
  (while true
    (doseq [[game all-bots] (db/with-conn (db/active-bots))]
      (let [player-1 (rand-nth all-bots)
            player-2 (->> all-bots
                          (remove (partial = player-1))
                          (sort-by (fn [bot] (Math/abs
                                               (- (:bot/rating bot)
                                                  (:bot/rating player-1)))))
                          (take 10)
                          (sort-by :bot/rating-dev #(compare %2 %1))
                          (take 5)
                          rand-nth)
            result (game-runner/run-game
                     game
                     [(-> (into {} player-1) (assoc :bot/deployd-code (db/deployed-code (:db/id player-1))))
                      (-> (into {} player-2) (assoc :bot/deployd-code (db/deployed-code (:db/id player-2))))])]
        (if-not (:error result)
          (let [match-info {:match/bots [player-1 player-2]
                            :match/moves (get-in result [:game-state "history"])}
                match-info (if-let [winner (:winner result)]
                             (assoc match-info :match/winner winner)
                             match-info)]
            (db/with-conn
              (db/create-entity match)
              (ranking/update-rankings player-1 player-2 (:winner result))
              ))
          )))))
