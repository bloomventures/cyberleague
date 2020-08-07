(ns cyberleague.db.seed
  (:require
   [datomic.api :as d]
   [cyberleague.db.core :as db]
   [cyberleague.game-registrar :as registrar]))

(defn make-entities []
  (let [users [{:user/id 38405
                :user/name "jamesnvc"}
               {:user/id 89664
                :user/name "rafd"}]]
    (concat
      ;; users
     users
      ;; games
     (for [[_ game-config] @registrar/games]
       (game-config :game.config/seed-game))
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

  (db/init)

  (doseq [entity (make-entities)]
    (cond
      (entity :user/id)
      (db/with-conn (db/create-user (entity :user/id) (entity :user/name)))

      (entity :game/name)
      (db/with-conn (db/create-game (entity :game/name) (entity :game/description)))

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
                  (db/create-bot user-id game-id))]
        (db/with-conn (db/update-bot-code (:db/id bot) (entity :bot/code)))
        (db/with-conn (db/deploy-bot (:db/id bot)))))))
