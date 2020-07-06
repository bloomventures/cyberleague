(ns cyberleague.server.seed
  (:require [cyberleague.coordinator.db :as db]))

(db/init)

(def goofspiel-description 
"Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.

3 suits are taken from the game, one is yours to play with, another is your opponent's, and the third is the trophy deck (what you fight over). Each round, one of the trophy cards is revealed, then you and your opponent simultaneously bid one of your cards. The player with the higher bid scores the value of the trophy card. If there's a tie, no one scores. The spent trophy and bid cards are removed, and the next round is played. Once all cards have been played, the scores are tallied: the winner is the player with the most points (the sum of the value of the trophy cards they scored).")

(def goofspiel-rules
"For our purposes, the game is played with the integers 1 through 12.

          ## Function Input:

          { :your-cards #{ 1 2 3 13 }
            :their-cards #{ 1 2 3 13 }
            :trophy-cards #{ 1 2 3 13 }
            :current-trophy 4
            :history [ { :you 1 :them 1 :trophy 1 } … ] } }

          ## Expected Output:

          a        ; a is the integer corresponding to your bid, it must be an integer that is still in your deck")

(def ultimate-tic-tac-toe-description
  "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master it's bigger brother.
  In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested a meta Tic-Tac-Toe game.")

(def users [(db/with-conn (db/create-user 38405 "jamesnvc"))
            (db/with-conn (db/create-user 89664 "rafd"))
            (db/with-conn (db/create-user 6969 "pizzani"))])
(def games {:goofspiel (db/with-conn (db/create-game "goofspiel" goofspiel-description))
            :ultimate-tic-tac-toe (db/with-conn (db/create-game "ultimate-tic-tac-toe" ultimate-tic-tac-toe-description))})
(def bot-code {:goofspiel [(pr-str '(fn [state]
                                      (let [trophy (state "current-trophy")]
                                        (get [2 3 4 5 6 7 8 9 10 11 12 13 1] (dec trophy)))))
                           (pr-str '(fn [state]
                                      (let [trophy (state "current-trophy")]
                                        (get [1 2 3 4 5 6 7 8 9 10 11 12 13] (dec trophy)))))
                           (pr-str '(fn [state]
                                      (let [trophy (state "current-trophy")] trophy)))
                           (pr-str '(fn [state]
                                      (rand-nth (vec (get-in state ["player-cards" "me"])))))]
               :ultimate-tic-tac-toe [(pr-str '(fn [state] state))]})

(defn create-bots-for-game [game]
  "create a bot for each user"
  (doseq [user users]
    (let [bot (db/with-conn (db/create-bot (:db/id user) (:db/id (games game))))]
      (db/with-conn (db/update-bot-code (:db/id bot) ((bot-code game) (rand-int (count (bot-code game))))))
      (db/with-conn (db/deploy-bot (:db/id bot))))))

(defn seed! []
  (map (fn [[game-name _]] ; game-name = :goofspiel or :ultimate-tic-tac-toe
    (create-bots-for-game game-name)) games))

(defn -main
  [& args]
  (seed!))
