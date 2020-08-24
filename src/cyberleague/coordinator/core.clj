(ns cyberleague.coordinator.core
  (:gen-class)
  (:require
   [datomic.api :as d]
   [cyberleague.db.core :as db]
   [cyberleague.coordinator.game-runner :as game-runner]
   [cyberleague.coordinator.ranking :as ranking]))

(defonce run? (atom false))

(defn run-one-game! [game all-bots]
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
        ;_ (print "Starting " (:db/id player-1) " vs " (:db/id player-2) "...")
        result (game-runner/run-game
                (into {} game)
                (db/with-conn
                  [(-> (into {} player-1) (assoc :db/id (:db/id player-1)
                                                 :bot/code (db/deployed-code (:db/id player-1))))
                   (-> (into {} player-2) (assoc :db/id (:db/id player-2)
                                                 :bot/code (db/deployed-code (:db/id player-2))))]))]
    (println (str (:db/id player-1) " vs " (:db/id player-2) ": " (:winner result)))
    (if (false? (:game.result/error result))
      ; TODO: handle ties?
      (db/with-conn
        (db/create-entity (merge
                            {:match/bots [(:db/id player-1) (:db/id player-2)]
                             :match/state-history (pr-str (result :game.result/state-history))
                             :match/moves (pr-str (result :game.result/history))}
                            ;; optionally add this in b/c datomic does not allow nils
                            (when-let [winner (:game.result/winner result)]
                              {:match/winner winner})))
        (ranking/update-rankings! player-1 player-2 (:game.result/winner result)))
      (if (= (:error result) :exception-executing)
        (let [errd-bot (if (= (:db/id player-1) (:bot result))
                         player-1 player-2)]
          (println "Exception executing, will disable:" (:db/id errd-bot) (:info result))
          (db/with-conn
            (d/transact db/*conn*
                        [[:db/retract (:db/id errd-bot) :bot/code-version (:bot/code-version errd-bot)]])))
        (let [[winner cheater] (if (= (get-in result [:move :bot]) (:db/id player-1))
                                 [player-2 player-1]
                                 [player-1 player-2])]
          (println "Bad move from " cheater)
          (db/with-conn
            (db/create-entity {:match/bots [(:db/id player-1) (:db/id player-2)]
                               :match/error true
                               :match/moves (pr-str (conj (get-in result [:game-state :history])
                                                          (get-in result [:move :move])))
                               :match/winner (:db/id winner)})
            (d/transact db/*conn*
                        [[:db/add (:db/id cheater) :bot/rating (Math/max 0 (- (:bot/rating cheater) 10))]
                         [:db/retract (:db/id cheater) :bot/code-version (:bot/code-version cheater)]])))))))

#_(let [[game bots] (first (db/with-conn (db/active-bots)))]
    (->> bots
         (take 2)
         (map (fn [bot]
                {:db/id (:db/id bot)
                 :bot/code (db/with-conn (db/deployed-code (:db/id bot)))}))
         (game-runner/run-game game)))

(defn run-games! []
  (println "Running games")
  (reset! run? true)
  (while @run?
    (doseq [[game all-bots] (db/with-conn (db/active-bots))]
      (run-one-game! game all-bots)
      (Thread/sleep 1000))))

(defn start! []
  (when (not @run?)
    (future (run-games!))))

(defn stop! []
  (reset! run? false))
