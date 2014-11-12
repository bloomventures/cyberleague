(ns cyberleague.seed
  (:require [pog.db :as db]))

(defn seed []

  (db/init)

  (def user-james (db/with-conn (db/create-user 38405 "jamesnvc")))

  (def user-raf (db/with-conn (db/create-user 89664 "rafd")))

  (def game-goofspiel
    (let [g {:name "goofspiel"
             :description
             "
  Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.

  3 suits are taken from the game, one is yours to play with, another is your opponent's, and the third is the trophy deck (what you fight over). Each round, one of the trophy cards is revealed, then you and your opponent simultaneously bid one of your cards. The player with the higher bid scores the value of the trophy card. If there's a tie, no one scores. The spent trophy and bid cards are removed, and the next round is played. Once all cards have been played, the scores are tallied: the winner is the player with the most points (the sum of the value of the trophy cards they scored)." }]
      (db/with-conn (db/create-game (g :name) (g :description)))))

  (def bot-goofspiel
    (let [bot (db/with-conn (db/create-bot (:db/id user-james) (:db/id game-goofspiel)))
          _ (db/with-conn (db/update-bot-code (:db/id bot) (pr-str '(fn [state]
                                                                      (let [trophy (state "current-trophy")]
                                                                        (get [2 3 4 5 6 7 8 9 10 11 12 13 1] (dec trophy)))))))]
      bot))

  (def bot-goofspiel-2
    (let [bot (db/with-conn (db/create-bot (:db/id user-raf) (:db/id game-goofspiel)))
          _ (db/with-conn (db/update-bot-code (:db/id bot) (pr-str '(fn [state]
                                                                      (let [trophy (state "current-trophy")]
                                                                        (get [1 2 3 4 5 6 7 8 9 10 11 12 13] (dec trophy)))))))]
      bot))

  (def bot-goofspiel-3
    (let [bot (db/with-conn (db/create-bot (:db/id user-james) (:db/id game-goofspiel)))
          _ (db/with-conn (db/update-bot-code (:db/id bot) (pr-str '(fn [state]
                                                                      (let [trophy (state "current-trophy")]
                                                                        trophy)))))]
      bot))

  (def bot-goofspiel-4
    (let [bot (db/with-conn (db/create-bot (:db/id user-raf) (:db/id game-goofspiel)))
          _ (db/with-conn (db/update-bot-code (:db/id bot) (pr-str '(fn [state]
                                                                      (rand-nth (vec (get-in state ["player-cards" (:db/id bot)])))))))]
      bot))


  (db/with-conn
    (doseq [bot [bot-goofspiel bot-goofspiel-2 bot-goofspiel-3 bot-goofspiel-4]]
      (db/deploy-bot (:db/id bot)))))

(defn -main
  [& args]
  (seed))
