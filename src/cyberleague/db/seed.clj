(ns cyberleague.db.seed
  (:require
   [datomic.api :as d]
   [cyberleague.db.core :as db]
   [cyberleague.game-registrar :as registrar]))

(defn make-entities []
  (let [users [{:user/github-id 0
                :user/name "alice"}
               {:user/github-id 1
                :user/name "bob"}]]
    (concat
      ;; users
     users
      ;; games
     (for [[_ game-config] @registrar/games]
       {:game/name (:game.config/name game-config)
        :game/description (:game.config/description game-config)
        :game/rules (:game.config/rules game-config)})
      ;; bots
     (mapcat (fn [game-config]
               (map
                (fn [bot-code user]
                  {:bot/user-name (user :user/name)
                   :bot/game-name (game-config :game.config/name)
                   :bot/code bot-code})
                (:game.config/seed-bots game-config)
                (cycle users)))
             (vals @registrar/games)))))

(defn seed! []
  (db/drop!)
  (db/init!)

  (doseq [entity (make-entities)]
    (cond
      (entity :user/github-id)
      (db/with-conn (db/create-user! (entity :user/github-id) (entity :user/name)))

      (entity :game/name)
      (db/with-conn (db/create-game! (entity :game/name) (entity :game/description)))

      (entity :bot/code)
      (let [user-id (first (db/with-conn
                               (d/q '[:find [?user-id ...]
                                      :in $ ?user-name
                                      :where [?user-id :user/name ?user-name]]
                                    (d/db db/*conn*)
                                    (entity :bot/user-name))))
              game-id  (first (db/with-conn
                                (d/q '[:find [?game-id ...]
                                       :in $ ?game-name
                                       :where [?game-id :game/name ?game-name]]
                                     (d/db db/*conn*)
                                     (entity :bot/game-name))))
              bot (db/with-conn
                    (db/create-bot! user-id game-id))]
          (db/with-conn (db/update-bot-code! (:db/id bot)
                                            (-> entity :bot/code :code/code)
                                            (-> entity :bot/code :code/language)))
          (db/with-conn (db/deploy-bot! (:db/id bot)))))))

#_(seed!)
