(ns cyberleague.db.seed
  (:require
   [tada.events.core :as tada]
   [bloom.commons.uuid :as uuid]
   [datomic.api :as d]
   [cyberleague.common.transit :as transit]
   [cyberleague.coordinator.ranking :as ranking]
   [cyberleague.server.cqrs :as cqrs]
   [cyberleague.db.core :as db]
   [cyberleague.db.matches :as matches]
   [cyberleague.common.transit-client :as http]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.common.envs :as envs]
   [cyberleague.server.evaluator-client :as eval-client]
   [cyberleague.game-registrar :as games]))

(defn uuid [x]
  (uuid/from-email (str x)))

(defn initialize-test-bots!
  []
  (let [user-id (uuid "admin")]
    (->> (games/all)
         (map (fn [game-config]
                (let [{:blueprint/keys [env-slug code]}
                      (:game.config/test-bot game-config)
                      bot-id (:bot/id (tada/do! cqrs/t
                                                :api/create-bot!
                                                {:user-id user-id
                                                 :game-slug (:game.config/slug game-config)
                                                 :env-slug env-slug}))
                      upload-url (:upload-url
                                  (tada/do! cqrs/t
                                            :api/artifact-upload-prepare!
                                            {:user-id user-id
                                             :bot-id bot-id
                                             :env-slug env-slug
                                             :weight 0
                                             :digest (artifact/digest code)}))]
                  (when upload-url
                    (http/file-upload-request
                     {:url upload-url
                      :method :post
                      :body code})))))
         doall)))

#_(initialize-test-bots!)

(defn check-test-bots!
  []
  (->> (games/all)
       (take 1)
       (map (fn [game-config]
              (let [game-engine (cyberleague.games.protocol/make-engine
                                 {:game/slug (:game.config/name game-config)})
                    state (cyberleague.games.protocol/init-state
                           game-engine [123 456])]
                (let [{:blueprint/keys [env-slug code]} (:game.config/test-bot game-config)]
                  (eval-client/eval!
                   {:digest (artifact/digest code)
                    :env-slug env-slug
                    :input
                    (clojure.data.json/write-str
                     (cyberleague.games.protocol/anonymize-state-for
                      game-engine 123 state))})))))))

(defn initialize-envs!
  []
  (db/with-conn
   (let [envs (envs/all)]
     ;; languages
     (doseq [language-slug (->> envs
                                (map :env/language-slug)
                                set)]
       (db/transact! [(db/language {:id (uuid language-slug)
                                    :slug language-slug})]))

     ;; envs
     (doseq [[env-slug language-slug]
             (->> envs
                  (map (fn [e]
                         [(:env/slug e)
                          (:env/language-slug e)])))]
       (db/transact! [(db/env {:id (uuid env-slug)
                               :slug env-slug
                               :language [:language/slug language-slug]})])))))

(defn initialize-core!
  []
  (db/with-conn
   (db/create-user! {:id (uuid "admin")
                     :github-id 0
                     :name "admin"})

   (initialize-envs!)

   ;; games
   (doseq [game-config (games/all)
           :let [game-id (uuid (:game.config/slug game-config))]]
     (db/transact!
      [{:game/id game-id
        :game/slug (:game.config/slug game-config)}]))

   ;; test-bots
   (initialize-test-bots!)))

(defn seed! []
  (db/drop!)
  (db/init!)

  (initialize-core!)

  (db/with-conn
   (doseq [params [{:id (uuid "alice")
                    :github-id 1
                    :name "alice"}
                   {:id (uuid "bob")
                    :github-id 2
                    :name "bob"}]]
     (db/create-user! params))

   (let [user-ids (d/q '[:find [?user-id ...]
                         :in $ ?admin-uuid
                         :where
                         [?u :user/id ?user-id]
                         [(not= ?user-id ?admin-uuid)]]
                       (d/db db/*conn*)
                       (uuid "admin"))]

     (doseq [game-config (games/all)]
       (doseq [[{:blueprint/keys [env-slug code]} user-id]
               (map vector
                    (:game.config/seed-bots game-config)
                    (cycle user-ids))]

         (let [bot-id (:bot/id (tada/do! cqrs/t
                                         :api/create-bot!
                                         {:user-id user-id
                                          :game-slug (:game.config/slug game-config)
                                          :env-slug env-slug}))
               upload-url (:upload-url
                           (tada/do! cqrs/t
                                     :api/artifact-upload-prepare!
                                     {:user-id user-id
                                      :bot-id bot-id
                                      :env-slug env-slug
                                      :weight 0
                                      :digest (artifact/digest code)}))]
           (when upload-url
             (http/file-upload-request
              {:url upload-url
               :method :post
               :body code}))

           (tada/do! cqrs/t
                     :api/deploy-bot!
                     {:user-id user-id
                      :bot-id bot-id
                      :digest (artifact/digest code)})))))

   ;; add some fake matches
   (doseq [[game bots] (->> (db/active-bots)
                            (group-by :bot/game))
           :when (>= (count bots) 2)]
     (doseq [[bot-1 bot-2] (partition 2 1 bots)]
       (dotimes [i 5]
         (let [bot-1 (db/by-id [:bot/id (:bot/id bot-1)])
               bot-2 (db/by-id [:bot/id (:bot/id bot-2)])
               artifact-1 (:bot/active-artifact bot-1)
               artifact-2 (:bot/active-artifact bot-2)
               winner (rand-nth [bot-1 bot-2 nil])
               winner-id (some-> winner :bot/id)
               match-id (uuid/random)
               ;; spread matches over the past few days
               match-time (java.util.Date. (- (System/currentTimeMillis)
                                              (* i 1000 60 60 12)))]
           (let [bot-ids [(:bot/id bot-1) (:bot/id bot-2)]
                match {:match/id match-id
                       :match/test? false
                       :match/game-id (:game/id game)
                       :match/bot-ids #{(:bot/id bot-1) (:bot/id bot-2)}
                       :match/artifact-ids #{(:artifact/id artifact-1) (:artifact/id artifact-2)}
                       :match/timestamp match-time
                       :match/player-mappings {(:bot/id bot-1) 0
                                               (:bot/id bot-2) 1}
                       :match/log [{:log-entry/state {}}]
                       :match/disqualified-bot-ids #{}
                       :match/winning-bot-ids (if winner-id #{winner-id} #{})}]
           (db/transact!
            (concat
             (matches/match-txs match bot-ids)
             (ranking/new-ratings
              bot-1
              bot-2
              (:artifact/digest artifact-1)
              (:artifact/digest artifact-2)
              winner-id))))))))))

#_(seed!)

