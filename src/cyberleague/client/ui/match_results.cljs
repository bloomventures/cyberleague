(ns cyberleague.client.ui.match-results
  (:require
    [cyberleague.game-registrar :as registrar]))

(defn match-results-view [match]
  (let [view (get-in @registrar/games
               [(get-in match [:match/game :game/name])
                :game.config/match-results-view])]
    [view match]))
