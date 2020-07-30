(ns cyberleague.games.ultimate-tic-tac-toe.seed
  (:require
   [clojure.java.io :as io]))

(def entities
  [{:game/name "ultimate tic-tac-toe"
    :game/description (str "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master it's bigger brother.\n"
                           "In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested a meta Tic-Tac-Toe game.")
    :game/rules ""}

   {:bot/user-name "jamesnvc"
    :bot/game-name "ultimate tic-tac-toe"
    :bot/code (slurp (io/resource "code/ultimate tic-tac-toe.cljs"))}

   {:bot/user-name "rafd"
    :bot/game-name "ultimate tic-tac-toe"
    :bot/code (slurp (io/resource "code/ultimate tic-tac-toe 2.cljs"))}])
