(ns cyberleague.db.seed
  (:require
   [clojure.java.io :as io]
   [cyberleague.db.core :as db]
   [datomic.api :as d]))

(def entities
  [{:user/id 38405
    :user/name "jamesnvc"}

   {:user/id 89664
    :user/name "rafd"}

   {:game/name "goofspiel"
    :game/description (str "Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.\n"
                           "3 suits are taken from the game, one is yours to play with, another is your opponent's, and the third is the trophy deck (what you fight over). Each round, one of the trophy cards is revealed, then you and your opponent simultaneously bid one of your cards. The player with the higher bid scores the value of the trophy card. If there's a tie, no one scores. The spent trophy and bid cards are removed, and the next round is played. Once all cards have been played, the scores are tallied: the winner is the player with the most points (the sum of the value of the trophy cards they scored).")
    :game/rules (str "For our purposes, the game is played with the integers 1 through 12.\n"
                     "## Function Input:\n"
                     "\n"
                     "{:your-cards #{ 1 2 3 13 }
                     :their-cards #{ 1 2 3 13 }
                     :trophy-cards #{ 1 2 3 13 }
                     :current-trophy 4
                     :history [ { :you 1 :them 1 :trophy 1 } … ] } }\n"
                     "\n"
                     "## Expected Output:\n"
                     "\n"
                     "a        ; a is the integer corresponding to your bid, it must be an integer that is still in your deck")}

   {:bot/user-name "jamesnvc"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (let [trophy (state :current-trophy)]
                           (get [2 3 4 5 6 7 8 9 10 11 12 13 1] (dec trophy)))))}

   {:bot/user-name "rafd"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (let [trophy (state :current-trophy)]
                           (get [1 2 3 4 5 6 7 8 9 10 11 12 13] (dec trophy)))))}

   {:bot/user-name "jamesnvc"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (let [trophy (state :current-trophy)]
                           trophy)))}

   {:bot/user-name "rafd"
    :bot/game-name "goofspiel"
    :bot/code (pr-str '(fn [state]
                         (rand-nth (vec (get-in state [:player-cards :me])))))}

   {:game/name "ultimate tic-tac-toe"
    :game/description (str "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master it's bigger brother.\n"
                           "In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested a meta Tic-Tac-Toe game.")
    :game/rules ""}

   {:bot/user-name "jamesnvc"
    :bot/game-name "ultimate tic-tac-toe"
    :bot/code (slurp (io/resource "code/ultimate tic-tac-toe.cljs"))}

   {:bot/user-name "rafd"
    :bot/game-name "ultimate tic-tac-toe"
    :bot/code (slurp (io/resource "code/ultimate tic-tac-toe 2.cljs"))}])

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
