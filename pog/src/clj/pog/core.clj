(ns pog.core
  (:require [pog.db :as db]
            [datomic.api :as d]
            [pog.ranking :as ranking]
            [pog.game-runner :as game-runner]))

(defn -main
  [& args]
  (println "Running games")
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
                     (into {} game)
                     (db/with-conn
                       [(-> (into {} player-1) (assoc :db/id (:db/id player-1)
                                                 :bot/deployed-code (db/deployed-code (:db/id player-1))))
                        (-> (into {} player-2) (assoc :db/id (:db/id player-2)
                                                 :bot/deployed-code (db/deployed-code (:db/id player-2))))]))]
        (println (str player-1 " vs " player-2 ": " result))
        (if-not (:error result)
          (let [match-info {:match/bots [(:db/id player-1) (:db/id player-2)]
                            :match/moves (get-in result [:game-state "history"])}
                match-info (if-let [winner (:winner result)]
                             (assoc match-info :match/winner winner)
                             match-info)]
            (db/with-conn
              (db/create-entity match-info)
              (ranking/update-rankings player-1 player-2 (:winner result))))
          (let [[winner cheater] (if (= (get-in result [:move :bot] (:db/id player-1)))
                                   [player-2 player-1]
                                   [player-1 player-2])]
            (db/with-conn
              (db/create-entity {:match/bots [player-1 player-2]
                                 :match/error true
                                 :match/moves (conj (get-in result [:game-state "history"])
                                                    (get-in result [:move :move]))
                                 :match/winner winner})
              (d/transact db/*conn*
                [[:db/add cheater :bot/rating (- 10 (:bot-ranking cheater))]]))))))))
