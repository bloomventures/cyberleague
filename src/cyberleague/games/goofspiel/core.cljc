(ns cyberleague.games.goofspiel.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.goofspiel.engine] ;; so it gets registered
   [cyberleague.games.goofspiel.ui :as ui]
   [cyberleague.games.goofspiel.seed :as seed]))

(cyberleague.game-registrar/register-game!
 {:game.config/name "goofspiel"
  :game.config/match-results-view ui/match-results-view
  :game.config/seed-bots seed/bots
  :game.config/seed-game seed/game})
