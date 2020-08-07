(ns cyberleague.games.ultimate-tic-tac-toe.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.ultimate-tic-tac-toe.engine] ;; so it gets registered
   [cyberleague.games.ultimate-tic-tac-toe.ui :as ui]
   [cyberleague.games.ultimate-tic-tac-toe.starter-code :as starter-code]
   [cyberleague.games.ultimate-tic-tac-toe.seed :as seed]))

(cyberleague.game-registrar/register-game!
 {:game.config/name "ultimate tic-tac-toe"
  :game.config/match-results-view ui/match-results-view
  :game.config/match-results-styles ui/>results-styles
  :game.config/starter-code starter-code/starter-code
  :game.config/seed-game seed/game
  :game.config/seed-bots seed/bots})
