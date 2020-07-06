(ns cyberleague.server.seed
  (:require [cyberleague.coordinator.db :as db]))

(def goofspiel-description 
             "
Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.

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

(defn seed! []
  (db/init)

  (let [user-james (db/with-conn (db/create-user 38405 "jamesnvc"))
        user-raf (db/with-conn (db/create-user 89664 "rafd"))
        game-goofspiel (db/with-conn (db/create-game "goofspiel" goofspiel-description))
        game-ultimate-tic-tac-toe (db/with-conn (db/create-game "Ultimate Tic-Tac-Toe" ultimate-tic-tac-toe-description))
        bot-goofspiel-1 (db/with-conn (db/create-bot (:db/id user-james) (:db/id game-goofspiel)))
        bot-goofspiel-2 (db/with-conn (db/create-bot (:db/id user-raf) (:db/id game-goofspiel)))
        bot-goofspiel-3 (db/with-conn (db/create-bot (:db/id user-james) (:db/id game-goofspiel)))
        bot-goofspiel-4 (db/with-conn (db/create-bot (:db/id user-raf) (:db/id game-goofspiel)))]
          (db/with-conn (db/update-bot-code (:db/id bot-goofspiel-1) (pr-str '(fn [state]
                                                                       (let [trophy (state "current-trophy")]
                                                                         (get [2 3 4 5 6 7 8 9 10 11 12 13 1] (dec trophy)))))))
          (db/with-conn (db/update-bot-code (:db/id bot-goofspiel-2) (pr-str '(fn [state]
                                                                      (let [trophy (state "current-trophy")]
                                                                        (get [1 2 3 4 5 6 7 8 9 10 11 12 13] (dec trophy)))))))
          (db/with-conn (db/update-bot-code (:db/id bot-goofspiel-3) (pr-str '(fn [state]
                                                                      (let [trophy (state "current-trophy")]
                                                                        trophy)))))
          (db/with-conn (db/update-bot-code (:db/id bot-goofspiel-4) (pr-str '(fn [state]
                                                                      (rand-nth (vec (get-in state ["player-cards" "me"])))))))
          (db/with-conn
            (doseq [bot [bot-goofspiel-1 bot-goofspiel-2 bot-goofspiel-3 bot-goofspiel-4]]
              (db/deploy-bot (:db/id bot))))))

(defn -main
  [& args]
  (seed!))
