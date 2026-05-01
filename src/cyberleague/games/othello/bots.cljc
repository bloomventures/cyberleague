(ns cyberleague.games.othello.bots)

(def first-valid-bot
  '(fn [{:keys [available-moves] :as state}]
     (if (:ping state)
       {:pong (:ping state)}
       (first available-moves))))

(def random-valid-bot
  '(fn [{:keys [available-moves]}]
     (if (:ping state)
       {:pong (:ping state)}
       (rand-nth available-moves))))
