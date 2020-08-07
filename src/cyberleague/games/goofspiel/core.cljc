(ns cyberleague.games.goofspiel.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.goofspiel.engine] ;; so it gets registered
   [cyberleague.games.goofspiel.bots :as bots]
   [cyberleague.games.goofspiel.ui :as ui]
   [cyberleague.games.goofspiel.seed :as seed]
   [cyberleague.games.goofspiel.starter-code :as starter-code]))

(cyberleague.game-registrar/register-game!
 {:game.config/name "goofspiel"
  :game.config/match-results-view ui/match-results-view
  :game.config/match-results-styles ui/>results-styles
  :game.config/starter-code starter-code/starter-code
  :game.config/test-bot (pr-str bots/random-bot)
  :game.config/seed-bots seed/bots
  :game.config/seed-game seed/game})
