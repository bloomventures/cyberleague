(ns cyberleague.games.ultimate-tic-tac-toe.seed
  (:require
   [cyberleague.games.ultimate-tic-tac-toe.bots :as bots]))

(def game
  {:game/name "ultimate tic-tac-toe"
   :game/description (str "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master it's bigger brother.\n"
                          "In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested a meta Tic-Tac-Toe game.")
   :game/rules ""})

(def bots
  [{:bot/user-name "jamesnvc"
    :bot/game-name "ultimate tic-tac-toe"
    :bot/code (pr-str bots/random-valid-bot)}

   {:bot/user-name "rafd"
    :bot/game-name "ultimate tic-tac-toe"
    :bot/code (pr-str bots/first-valid-bot)}])
