(ns cyberleague.coordinator.core
  (:gen-class)
  (:require
   [bloom.commons.uuid :as uuid]
   [hyperfiddle.rcf :as rcf]
   [cyberleague.common.config :refer [config]]
   [cyberleague.coordinator.game-runner :as game-runner]
   [cyberleague.coordinator.ranking :as ranking]
   [cyberleague.db.core :as db]
   [cyberleague.common.transit :as t]))

(defn run-game!
  [{:keys [game bots artifacts test?]}]
  #_(print "Starting " (:bot/id player-1) " vs " (:bot/id player-2) "...")
  (db/with-conn
   (let [result (game-runner/run-game
                 {:game (into {} game)
                  :bot-ids (mapv :bot/id bots)
                  :artifacts artifacts})
         [player-1 player-2] bots]
     #_(println (str (:bot/id player-1) " vs " (:bot/id player-2) ": " (:game.result/winner result)))
     (let [{:move.error/keys [data _type] :as error} (:game.result/error result)
           ;; assuming 2 player games
           [winning-bot errd-bot] (when error
                                    (if (= (:bot-id data)
                                           (:bot/id player-1))
                                      [player-2 player-1]
                                      [player-1 player-2]))
           ;; run-game returns game.result/winner only for completed games
           ;; but for tournament purposes, if a bot errors
           ;; we still mark a winner, so that erroring is not a viable "cheesing" strat
           ;; (ie. when about to lose, error)
           winner-id (or (:game.result/winner result)
                         (:bot/id winning-bot))
           match-id (uuid/random)]
       (when (not test?)
         (when errd-bot
           (println "Disabling bot:" (:bot/id errd-bot) (:message data))
           (db/disable-bot! errd-bot)))

       (db/transact! [(merge
                       {:match/id match-id
                        :match/test? test?
                        :match/bots [[:bot/id (:bot/id player-1)]
                                     [:bot/id (:bot/id player-2)]]
                        :match/timestamp (java.util.Date.)
                            :match/state-history-transit (t/write-str (:game.result/state-history result))
                            :match/std-out-history-transit (t/write-str (:game.result/std-out-history result))
                            :match/moves-transit (t/write-str (:game.result/history result))}
                       (when error
                             {:match/error-transit (t/write-str error)})
                       (when winner-id
                         {:match/winner [:bot/id winner-id]}))])

       (when (not test?)
         (ranking/update-rankings! player-1 player-2 winner-id))

       {:match/id match-id}))))

(defn select-players
  [active-bots]
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
 (select-players []) := nil
 (select-players [{:bot/rating 100}]) := nil
 (select-players [{:bot/rating 100} {:bot/rating 200}]) := {:player-1 _ :player-2 _})

(defn run-one-game!
  [game active-bots]
  (when-let [{:keys [player-1 player-2]} (select-players active-bots)]
    (run-game! {:game game
                :bots [player-1 player-2]
                :artifacts (mapv :bot/active-artifact [player-1 player-2])
                :test? false})))

#_(let [[game bots] (first (db/with-conn (db/active-bots)))]
    (->> bots
         (take 2)
         (map (fn [bot]
                {:bot/id (:bot/id bot)
                 :bot/code (db/with-conn (db/deployed-code [:bot/id (:bot/id bot)]))}))
         (game-runner/run-game game)))

;; -------

(defonce run? (atom false))

(defn run-games! []
  (println "Running games")
  (reset! run? true)
  (while @run?
    (doseq [[game active-bots] (db/with-conn (db/active-bots))]
      (run-one-game! game active-bots)
      (Thread/sleep (-> config :server :coordinator-delay)))))

(defn start! []
  (when (not @run?)
    (future (run-games!))))

(defn stop! []
  (reset! run? false))
