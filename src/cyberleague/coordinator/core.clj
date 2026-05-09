(ns cyberleague.coordinator.core
  (:gen-class)
  (:require
   [bloom.commons.uuid :as uuid]
   [hyperfiddle.rcf :as rcf]
   [taoensso.telemere :as tel]
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
                  (if (= :eval.error.origin/system (-> result :eval/error :eval.error/origin))
                    result
                    (merge result
                           (when (not= nonce (:pong (:eval/return-value result)))
                             {:eval/error {:eval.error/type :eval.error.type/failed-ping-pong
                                           :eval.error/origin :eval.error.origin/bot}})))])))
       (into {})))

(defn run-game!
  [{:keys [game bots artifacts test?]}]
  (tel/trace!
   {:id ::game
    :level :info
    :data {:game game
           :bot-names (map :bot/name bots)
           :bots bots
           :test? test?}}
   (let [match-id (uuid/random)]
     (db/with-conn
      (let [ping-pong-evals (ping-pong! bots artifacts)
            ping-pong-system-errors (->> ping-pong-evals
                                         (filter (fn [[_ eval]]
                                                   (= :eval.error.origin/system
                                                      (-> eval :eval/error :eval.error/origin))))
                                         (into {}))
            ping-pong-errors (->> ping-pong-evals
                                  (filter (fn [[_ eval]]
                                            (= :eval.error.origin/bot
                                               (-> eval :eval/error :eval.error/origin))))
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
          (tel/event! ::ping-pong-system-error
                      {:level :error
                       :data {:errors ping-pong-system-errors}})

          (seq ping-pong-errors)
          (do
            (tel/log! "Storing match (ping pong fail)")
            @(db/transact! (db.matches/match-txs
                           (assoc match
                                  :match/disqualified-bot-ids
                                  (set (keys ping-pong-errors))
                                  :match/log [{:log-entry/evals ping-pong-evals}])
                           (map :bot/id bots)))
            (when (not test?)
              (doseq [errd-bot-id (keys ping-pong-errors)]
                (tel/log! (str "Disabling bot (ping-pong fail):" errd-bot-id))
                (db/disable-bot! errd-bot-id (->> (map vector bots artifacts)
                                                  (some (fn [[bot artifact]]
                                                          (when (= (:bot/id bot) errd-bot-id)
                                                            (:artifact/id artifact)))))))))

          :else
          (let [result (game-runner/run-game
                        {:game-slug (:game/slug game)
                         :bot-ids (mapv :bot/id bots)
                         :artifacts artifacts})
                errors (:game.result/errors result)]
            (if (some (fn [[_ error]]
                        (= :eval.error.origin/system (:eval.error/origin error)))
                      errors)
              (tel/event! ::game-system-error {:level :error
                                              :data {:errors errors}})
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
                    (tel/log! (str "Disabling bot:" (:bot/id errd-bot)))
                    (db/disable-bot! (:bot/id errd-bot) (:artifact/id (:bot/active-artifact errd-bot)))))

                (tel/log! "Storing match")
                @(db/transact!
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
        {:match/id match-id})))))

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
          n (count active-bots)
          player-2-candidates (->> active-bots
                                   (remove (fn [bot] (= user-1-id (-> bot :bot/user :user/id))))
                                   (sort-by (fn [bot] (Math/abs
                                                       (- (:bot/rating bot)
                                                          (:bot/rating player-1)))))
                                   (take (max 1 (quot n 10)))
                                   (sort-by :bot/rating-dev #(compare %2 %1))
                                   (take (max 1 (quot n 20))))]
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

(defn pick-game-slug []
  ;; this biases (by design) towards games with more bots
  (when-let [active-bots (seq (db/active-bots))]
    (:game/slug (:bot/game (rand-nth active-bots)))))

(defn matchmake! [game-slug]
  (when-let [active-game-bots (->> (db/active-bots)
                                   (filter (fn [b]
                                             (= game-slug (:game/slug (:bot/game b)))))
                                   seq)]
    (run-one-game! (db/by-id [:game/slug game-slug]) active-game-bots)))

#_(db/with-conn (matchmake! "liars-dice"))

;; -------

(defonce executor (atom nil))

#_(identity executor)

(defn start! []
  (when (nil? @executor)
    (tel/log! "Starting coordinator")
    (let [exec (java.util.concurrent.ScheduledThreadPoolExecutor. 1)]
      (reset! executor exec)
      (.scheduleWithFixedDelay
       exec
       (fn []
         (try
           (db/with-conn
            (matchmake! (pick-game-slug)))
           (catch Exception e
             (tel/error! ::match-error e))))
       0
       (-> config :server :coordinator-delay)
       java.util.concurrent.TimeUnit/MILLISECONDS))))

(defn status []
  (if-let [exec @executor]
    {:running? (not (.isShutdown exec))
     :terminated? (.isTerminated exec)
     :queued (.. exec getQueue size)
     :active (.getActiveCount exec)}
    {:running? false
     :terminated? true
     :queued 0
     :active 0}))

#_(status)

(defn stop! []
  (when-let [exec @executor]
    (tel/log! "Stopping coordinator")
    (.shutdown exec)
    (reset! executor nil)))

#_(stop!)
