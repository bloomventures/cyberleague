(ns cyberleague.games.ultimate-tic-tac-toe.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.ultimate-tic-tac-toe.engine] ;; so it gets registered
   [cyberleague.games.ultimate-tic-tac-toe.ui :as ui]
   [cyberleague.games.ultimate-tic-tac-toe.bots :as bots]
   [cyberleague.games.ultimate-tic-tac-toe.starter-code :as starter-code]))

(cyberleague.game-registrar/register-game!
 {:game.config/name "ultimate tic-tac-toe"
  :game.config/description
  (str "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master it's bigger brother.\n"
       "In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested a meta Tic-Tac-Toe game.")
  :game.config/rules ""
  :game.config/match-results-view ui/match-results-view
  :game.config/match-results-styles ui/>results-styles
  :game.config/starter-code starter-code/starter-code
  :game.config/test-bot (pr-str bots/random-valid-bot)
  :game.config/seed-bots [(pr-str bots/random-valid-bot)
                          (pr-str bots/first-valid-bot)]})
