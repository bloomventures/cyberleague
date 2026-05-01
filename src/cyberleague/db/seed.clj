(ns cyberleague.db.seed
  (:require
   [tada.events.core :as tada]
   [bloom.commons.uuid :as uuid]
   [datomic.api :as d]
   [cyberleague.server.cqrs :as cqrs]
   [cyberleague.db.core :as db]
   [cyberleague.common.transit-client :as http]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.common.envs :as envs]
   [cyberleague.server.evaluator-client :as eval-client]
   [cyberleague.game-registrar :as registrar]))

(defn uuid [x]
  (uuid/from-email (str x)))

(defn initialize-test-bots!
  []
  (let [user-id (uuid "admin")]
    (->> @registrar/games
         vals
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
         doall

         #_(->> @registrar/games
                vals
                (map :game.config/test-bot)
                (map (fn [{:blueprint/keys [_env-slug code]}]
                       (let [result (eval-client/prepare {:digest (artifact/digest code)})]
                         (if (:skip? result)
                           (println "Skipping")
                           (do
                             (println "Uploading")
                             (http/file-upload-request
                              {:url (:upload-url result)
                               :method :post
                               :body code}))))))
                doall))))

#_(initialize-test-bots!)

(defn check-test-bots!
  []
  (->> @registrar/games
       vals
       (take 1)
       (map (fn [game-config]
              (let [game-engine (cyberleague.games.protocol/make-engine
                                 {:game/name (:game.config/name game-config)})
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

(defn initialize-core!
  []
  (db/with-conn
   (db/create-user! {:id (uuid "admin")
                     :github-id 0
                     :name "admin"})

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
                               :language [:language/slug language-slug]})])))

   ;; games
   (doseq [[_ game-config] @registrar/games
           :let [game-id (uuid (:game.config/name game-config))]]
     (db/transact!
      [{:game/id game-id
        :game/name (:game.config/name game-config)
        :game/slug (:game.config/slug game-config)
        :game/description (:game.config/description game-config)
        :game/rules (:game.config/rules game-config)}]))

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

     (doseq [[_ game-config] @registrar/games]
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
                      :digest (artifact/digest code)})))))))

#_(seed!)

