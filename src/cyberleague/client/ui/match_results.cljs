(ns cyberleague.client.ui.match-results
  (:require
    [cyberleague.games.goofspiel.ui :as goofspiel.ui]
    [cyberleague.games.ultimate-tic-tac-toe.ui :as ultimate-tic-tac-toe.ui]))

(defn match-results-view [match]
  (case (get-in match [:game :name])
    "goofspiel"
    [goofspiel.ui/match-results-view match]
    "ultimate tic-tac-toe"
    [ultimate-tic-tac-toe.ui/match-results-view match]
    ;; default
    [:div (str "Moves: " (:moves match))]))
