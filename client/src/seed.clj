(ns seed)

(require '[cyberleague.db :as db])

(db/init)

(def game (db/with-conn (db/create-game "goofspiel" "Game of Goofspiel" "Rules of Goofspiel")))

(def user1 (db/with-conn (db/create-user "" "james")))
(def simple-bot (db/with-conn (db/create-bot (:db/id user1) (:db/id game))))

(def user2 (db/with-conn (db/create-user "" "rafal")))
(def simple-bot-2 (db/with-conn (db/create-bot (:db/id user2) (:db/id game))))

(db/with-conn
  (db/update-bot-code
                (:db/id simple-bot)
                "(fn [state] (state \"current-trophy\"))")
  (db/update-bot-code
    (:db/id simple-bot-2)
    (str "(fn [state] "
         "(rand-nth (vec (get-in state [\"player-cards\"" (:db/id simple-bot-2) "]))))")
    ))
