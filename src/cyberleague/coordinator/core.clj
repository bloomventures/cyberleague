(ns cyberleague.coordinator.core
  (:gen-class)
  (:require
   [hyperfiddle.rcf :as rcf]
   [cyberleague.config :refer [config]]
   [cyberleague.coordinator.game-runner :as game-runner]
   [cyberleague.coordinator.ranking :as ranking]
   [cyberleague.db.core :as db]))

(defonce run? (atom false))

(defn select-players
  [_game active-bots]
  (when (seq active-bots) ;; possible to have no active bots
    (let [player-1 (rand-nth active-bots)
          player-2-candidates (->> active-bots
                                   (remove (partial = player-1))
                                   (sort-by (fn [bot] (Math/abs
                                                       (- (:bot/rating bot)
                                                          (:bot/rating player-1)))))
                                   (take 10)
                                   (sort-by :bot/rating-dev #(compare %2 %1))
                                   (take 5))]
      (when (seq player-2-candidates) ;; possible to have no other candidates
        (let [player-2 (rand-nth player-2-candidates)]
          {:player-1 player-1
           :player-2 player-2})))))

#_(apply select-players (first (db/with-conn (db/active-bots))))

(rcf/tests
 (select-players nil []) := nil
 (select-players nil [{:bot/rating 100}]) := nil
 (select-players nil [{:bot/rating 100} {:bot/rating 200}]) := {:player-1 _ :player-2 _})

(defn run-one-game! [game active-bots]
  (when-let [{:keys [player-1 player-2]} (select-players game active-bots)]
    #_(print "Starting " (:db/id player-1) " vs " (:db/id player-2) "...")
    (let [result (game-runner/run-game
                  (into {} game)
                  (db/with-conn
                   [(-> (into {} player-1) (assoc :db/id (:db/id player-1)
                                                  :bot/code (db/deployed-code (:db/id player-1))))
                    (-> (into {} player-2) (assoc :db/id (:db/id player-2)
                                                  :bot/code (db/deployed-code (:db/id player-2))))]))]
      #_(println (str (:db/id player-1) " vs " (:db/id player-2) ": " (:game.result/winner result)))
      (let [{:move.error/keys [data _type] :as error} (:game.result/error result)
            ;; TODO assuming 2 player games
            [winning-bot errd-bot] (when error
                                     (if (= (:bot-id data) (:db/id player-1))
                                       [player-2 player-1]
                                       [player-1 player-2]))
            ;; run-game returns game.result/winner only for completed games
            ;; but for tournament purposes, if a bot errors
            ;; we still mark a winner, so that erroring is not a viable "cheesing" strat
            ;; (ie. when about to lose, error)
            winner-id (or (:game.result/winner result)
                          (:db/id winning-bot))]
        (when errd-bot
          (println "Disabling bot:" (:db/id errd-bot) (:message data))
          (db/disable-bot! errd-bot))

        (db/with-conn
         (db/create-entity! (merge
                             {:match/bots [(:db/id player-1) (:db/id player-2)]
                              :match/state-history (pr-str (:game.result/state-history result))
                              :match/std-out-history (pr-str (:game.result/std-out-history result))
                              :match/moves (pr-str (:game.result/history result))}
                             (when error
                               {:match/error (pr-str error)})
                             (when winner-id
                               {:match/winner winner-id})))
         (ranking/update-rankings! player-1 player-2 winner-id))))))

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
    (doseq [[game active-bots] (db/with-conn (db/active-bots))]
      (run-one-game! game active-bots)
      (Thread/sleep (config :coordinator-delay)))))

(defn start! []
  (when (not @run?)
    (future (run-games!))))

(defn stop! []
  (reset! run? false))
