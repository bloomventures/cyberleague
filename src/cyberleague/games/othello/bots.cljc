(ns cyberleague.games.othello.bots)

(def first-valid-bot
  '(fn [{:keys [available-moves]}]
    (first available-moves)))

(def random-valid-bot
  '(fn [{:keys [available-moves]}]
     (rand-nth available-moves)))
