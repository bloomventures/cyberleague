(ns cyberleague.db.seed
  (:require
   [tada.events.core :as tada]
   [bloom.commons.uuid :as uuid]
   [datomic.api :as d]
   [cyberleague.server.cqrs :as cqrs]
   [cyberleague.db.core :as db]
   [cyberleague.game-registrar :as registrar]))

(defn uuid [x]
  (uuid/from-email (str x)))

(defn seed! []
  (db/drop!)
  (db/init!)

  (db/with-conn
   (doseq [params [{:id (uuid "alice")
                    :github-id 0
                    :name "alice"}
                   {:id (uuid "bob")
                    :github-id 1
                    :name "bob"}]]
     (db/create-user! params))

   (let [user-ids (d/q '[:find [?user-id ...]
                         :where
                         [?u :user/id ?user-id]]
                       (d/db db/*conn*))]

     (doseq [[_ game-config] @registrar/games
             :let [game-id (uuid (:game.config/name game-config))]]
       (db/transact!
        [{:game/id game-id
          :game/name (:game.config/name game-config)
          :game/description (:game.config/description game-config)
          :game/rules (:game.config/rules game-config)}])

       (doseq [[bot-code user-id] (map vector
                                       (:game.config/seed-bots game-config)
                                       (cycle user-ids))]

         (let [bot-id (:bot/id (tada/do! cqrs/t
                                         :api/create-bot!
                                         {:user-id user-id
                                          :game-id game-id}))]
           (tada/do! cqrs/t
                     :api/set-bot-language!
                     {:user-id user-id
                      :bot-id bot-id
                      :language (:code/language bot-code)})
           (tada/do! cqrs/t
                     :api/set-bot-code!
                     {:user-id user-id
                      :bot-id bot-id
                      :code (:code/code bot-code)})
           (tada/do! cqrs/t
                     :api/deploy-bot!
                     {:user-id user-id
                      :bot-id bot-id})))))))

#_(seed!)

