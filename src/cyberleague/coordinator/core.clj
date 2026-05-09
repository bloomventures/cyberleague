(ns cyberleague.coordinator.core
  (:gen-class)
  (:require
   [bloom.commons.uuid :as uuid]
   [hyperfiddle.rcf :as rcf]
   [cyberleague.common.config :refer [config]]
   [cyberleague.coordinator.game-runner :as game-runner]
   [cyberleague.coordinator.ranking :as ranking]
   [cyberleague.db.core :as db]
   [cyberleague.db.matches :as db.matches]))

(defn ping-pong!
  "Evals bots for ping/pong; returns {bot-id eval}"
  [bots artifacts]
  (->> (map vector bots artifacts)
       (keep (fn [[bot artifact]]
               (let [nonce (str (uuid/random))
                     result (game-runner/eval-move
                             artifact
                             {:ping nonce})]
                 [(:bot/id bot)
                  (if (= :eval.error.type/system-error (-> result :eval/error :eval.error/type))
                    result
                    (merge result
                           (when (not= nonce (:pong (:eval/return-value result)))
                             {:eval/error {:eval.error/type :eval.error.type/failed-ping-pong}})))])))
       (into {})))

(defn run-game!
  [{:keys [game bots artifacts test?]}]
  (print "Starting " (:bot/name (first bots)) " vs " (:bot/name (second bots)) "...")
  (let [match-id (uuid/random)]
    (db/with-conn
     (let [ping-pong-evals (ping-pong! bots artifacts)
           ping-pong-system-errors (->> ping-pong-evals
                                        (filter (fn [[_ eval]]
                                                  (= :eval.error.type/system-error
                                                     (-> eval :eval/error :eval.error/type))))
                                        (into {}))
           ping-pong-errors (->> ping-pong-evals
                                 (filter (fn [[_ eval]]
                                           (and (:eval/error eval)
                                                (not= :eval.error.type/system-error
                                                      (-> eval :eval/error :eval.error/type)))))
                                 (into {}))
           match {:match/id match-id
                  :match/game-id (:game/id game)
                  :match/test? test?
                  :match/bot-ids (->> bots
                                      (map :bot/id)
                                      set)
                  :match/artifact-ids (->> artifacts
                                           (map :artifact/id)
                                           set)
                  :match/timestamp (java.util.Date.)}]
       (cond
         (seq ping-pong-system-errors)
         (println "Skipping match (system error during ping-pong):" ping-pong-system-errors)

         (seq ping-pong-errors)
         (do
           (db/transact! (db.matches/match-txs
                          (assoc match
                                 :match/disqualified-bot-ids
                                 (set (keys ping-pong-errors))
                                 :match/log [{:log-entry/evals ping-pong-evals}]))
                         (map :bot/id bots))
           (when (not test?)
             (doseq [errd-bot-id (keys ping-pong-errors)]
               (println "Disabling bot (ping-pong fail):" errd-bot-id)
               (db/disable-bot! errd-bot-id (->> (map vector bots artifacts)
                                                 (some (fn [[bot artifact]]
                                                         (when (= (:bot/id bot) errd-bot-id)
                                                           (:artifact/id artifact)))))))))

         :else
         (let [result (game-runner/run-game
                       {:game (into {} game)
                        :bot-ids (mapv :bot/id bots)
                        :artifacts artifacts})
               errors (:game.result/errors result)]
           #_(println (str (:bot/id player-1) " vs " (:bot/id player-2) ": " (:game.result/winner result)))
           (if (some (fn [[_ error]]
                       (= :eval.error.type/system-error (:eval.error/type error)))
                     errors)
             (println "Skipping match (system error during game):" errors)
             (let [bots-by-status (->> bots
                                       (group-by (fn [b]
                                                   (if (contains? errors (:bot/id b))
                                                     ::errored
                                                     ::ok))))
                   ;; run-game returns game.result/winner only for completed games
                   ;; but for tournament purposes, if a bot errors
                   ;; we still mark a winner, so that erroring is not a viable "cheesing" strat
                   ;; (ie. when about to lose, error)
                   winner-ids (cond
                                (:game.result/winner result)
                                ;; game currently always returns single
                                [(:game.result/winner result)]

                                (seq (::ok bots-by-status))
                                ;; assuming 2 player games
                                [(:bot/id (first (::ok bots-by-status)))])]
               (when (not test?)
                 (doseq [errd-bot (::errored bots-by-status)]
                   (println "Disabling bot:" (:bot/id errd-bot) errors)
                   (db/disable-bot! (:bot/id errd-bot) (:artifact/id (:bot/active-artifact errd-bot)))))

               (db/transact!
                (concat
                 (db.matches/match-txs (assoc match
                                              :match/player-mappings
                                              (:game.result/player-mappings result)
                                              :match/log
                                              (:game.result/log result)
                                              :match/disqualified-bot-ids
                                              (->> (bots-by-status ::errored)
                                                   (map :bot/id)
                                                   set)
                                              :match/winning-bot-ids
                                              (set winner-ids))
                                       (map :bot/id bots))
                 (when (not test?)
                   (ranking/new-ratings
                    (first bots)
                    (second bots)
                    (:artifact/digest (first artifacts))
                    (:artifact/digest (second artifacts))
                    ;; curently only makes sense for 2p games anyway
                    (first winner-ids)))))))))
    {:match/id match-id}))))

(defn select-players
  [active-bots]
  ;; PROPERTIES WE WANT:
  ;; - per-game user match parity
  ;;     within each game, every user's bots collectively play the same number of matches
  ;;     user with 5 bots in game X has their bots play appx same # of games as user with 1 bot
  ;; - a user's bots never play against each other
  ;; - slightly prefer matchmaking against closer elo
  ;; PLAN
  ;;  group active-bots by user
  ;;  pick a user randomly
  ;;  pick a bot from user-bots randomly
  ;;  from active-bots, find bots-not-owned-by-user, then pick one with close-ish elo

  (when (seq active-bots)
    (let [bots-by-user (group-by (fn [bot] (-> bot :bot/user :user/id)) active-bots)
          user-1-id (rand-nth (keys bots-by-user))
          player-1 (rand-nth (get bots-by-user user-1-id))
          player-2-candidates (->> active-bots
                                   (remove (fn [bot] (= user-1-id (-> bot :bot/user :user/id))))
                                   (sort-by (fn [bot] (Math/abs
                                                       (- (:bot/rating bot)
                                                          (:bot/rating player-1)))))
                                   (take 10)
                                   (sort-by :bot/rating-dev #(compare %2 %1))
                                   (take 5))]
      (when (seq player-2-candidates)
        {:player-1 player-1
         :player-2 (rand-nth player-2-candidates)}))))

#_(apply select-players (first (db/with-conn (db/active-bots))))

(rcf/tests
 (select-players []) := nil
 (select-players [{:bot/rating 100 :bot/user {:user/id 1}}]) := nil
 (select-players [{:bot/rating 100 :bot/user {:user/id 1}}
                  {:bot/rating 100 :bot/user {:user/id 1}}]) := nil
 (select-players [{:bot/rating 100 :bot/user {:user/id 1}}
                  {:bot/rating 200 :bot/user {:user/id 2}}]) := {:player-1 _ :player-2 _})

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

(defn matchmake! []
  (db/with-conn
   (let [active-bots (db/active-bots)]
     (when (seq active-bots)
       ;; this biases (by design) towards games with more bots
       (let [game (:bot/game (rand-nth active-bots))]
         (try
           (run-one-game! game active-bots)
           (catch Exception e
             (println "Exception" e))))))))

#_(matchmake!)

;; -------

(defonce run? (atom false))

(defn run-games! []
  (println "Running games")
  (reset! run? true)
  (while @run?
    (matchmake!)
    (Thread/sleep (-> config :server :coordinator-delay))))

(defn start! []
  (when (not @run?)
    (future (run-games!))))

(defn stop! []
  (reset! run? false))
