(ns cyberleague.db.seed
  (:require
   [datomic.api :as d]
   [cyberleague.db.core :as db]
   [cyberleague.games.goofspiel.seed :as goofspiel.seed]
   [cyberleague.games.ultimate-tic-tac-toe.seed :as ultimate-tic-tac-toe.seed]))

(def entities
  (concat
   [{:user/id 38405
     :user/name "jamesnvc"}

    {:user/id 89664
     :user/name "rafd"}]
   goofspiel.seed/entities
   ultimate-tic-tac-toe.seed/entities))

(defn seed! []

  (db/init)

  (doseq [entity entities]
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
