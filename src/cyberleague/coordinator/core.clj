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

(defn ping-pong!
  "Evals bots for ping/pong; returns {bot-id move-error}"
  [bots artifacts]
  (->> (map vector bots artifacts)
       (keep (fn [[bot artifact]]
               (let [nonce (str (uuid/random))
                     result (game-runner/eval-move
                             artifact
                             {:ping nonce})]
                 [(:bot/id bot)
                  (merge result
                         (when (not= nonce (:pong (:eval/return-value result)))
                           {:eval/error {:move.error/type :move.error.type/failed-ping-pong}}))])))
       (into {})))

(defn run-game!
  [{:keys [game bots artifacts test?]}]
  #_(print "Starting " (:bot/id player-1) " vs " (:bot/id player-2) "...")
  (let [match-id (uuid/random)
        [player-1 player-2] bots]
    (db/with-conn
     (let [ping-pong-evals (ping-pong! bots artifacts)
           ping-pong-errors (->> ping-pong-evals
                                 (filter (fn [[_ eval]]
                                           (:eval/error eval)))
                                 (into {}))]
       (if (seq ping-pong-errors)
         (do
           (db/transact! [{:match/id match-id
                           :match/test? test?
                           :match/bots [[:bot/id (:bot/id player-1)]
                                        [:bot/id (:bot/id player-2)]]
                           :match/disqualified-bots
                           (->> (keys ping-pong-errors)
                                (map (fn [bot-id]
                                       [:bot/id bot-id])))
                           :match/timestamp (java.util.Date.)
                           :match/log-transit (t/write-str [{:log-entry/evals ping-pong-evals}])}])
           (when (not test?)
             (doseq [errd-bot-id (keys ping-pong-errors)]
               (println "Disabling bot (ping-pong fail):" errd-bot-id)
               (db/disable-bot! errd-bot-id (->> (map vector bots artifacts)
                                                 (some (fn [[bot artifact]]
                                                         (when (= (:bot/id bot) errd-bot-id)
                                                           artifact))))))))

         (let [result (game-runner/run-game
                       {:game (into {} game)
                        :bot-ids (mapv :bot/id bots)
                        :artifacts artifacts})]
           #_(println (str (:bot/id player-1) " vs " (:bot/id player-2) ": " (:game.result/winner result)))
           (let [errors (:game.result/errors result)
                 bots-by-status (->> bots
                                     (group-by (fn [b]
                                                 (if (contains? errors (:bot/id b))
                                                   ::errored
                                                   ::ok))))
                 ;; run-game returns game.result/winner only for completed games
                 ;; but for tournament purposes, if a bot errors
                 ;; we still mark a winner, so that erroring is not a viable "cheesing" strat
                 ;; (ie. when about to lose, error)
                 winner-ids (if (:game.result/winner result)
                              ;; game currently always returns single
                              [(:game.result/winner result)]

                              ;; assuming 2 player games
                              [(:bot/id (first (::ok bots-by-status)))])]
             (when (not test?)
               (doseq [errd-bot (::errored bots-by-status)]
                 (println "Disabling bot:" (:bot/id errd-bot) errors)
                 (db/disable-bot! (:bot/id errd-bot) (:bot/active-artifact errd-bot))))

             (db/transact! [(merge
                             {:match/id match-id
                              :match/test? test?
                              :match/bots [[:bot/id (:bot/id player-1)]
                                           [:bot/id (:bot/id player-2)]]
                              :match/timestamp (java.util.Date.)
                              :match/player-mappings-transit (t/write-str (:game.result/player-mappings result))
                              :match/log-transit (t/write-str (:game.result/log result))
                              :match/disqualified-bots
                              (->> (bots-by-status ::errored)
                                   (map (fn [id]
                                          [:bot/id (:bot/id id)])))
                              :match/winning-bots (->> winner-ids
                                                       (map (fn [id]
                                                              [:bot/id id])))})])

             (when (not test?)
               (ranking/update-rankings! player-1 player-2
                                         ;; curently only makes sense for 2p games anyway
                                         (first winner-ids))))))))
    {:match/id match-id}))


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
