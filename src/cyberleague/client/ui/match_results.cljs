(ns cyberleague.client.ui.match-results
  (:require
    [cyberleague.client.ui.goofspiel :as goofspiel]
    [cyberleague.client.ui.ultimate-tic-tac-toe :as ultimate-tic-tac-toe]))

(defn match-results-view [match]
  (case (get-in match [:game :name])
    "goofspiel"
    [goofspiel/match-results-view match]
    "ultimate tic-tac-toe"
    [ultimate-tic-tac-toe/match-results-view match]
    ;; default
    [:div (str "Moves: " (:moves match))]))
