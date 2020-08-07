(ns cyberleague.games.goofspiel.bots)

(def random-bot
  '(fn [state]
     (rand-nth (vec (get-in state [:player-cards :me])))))

(def current-trophy-bot
  '(fn [state]
     (let [trophy (state :current-trophy)]
       trophy)))

(def other-bot
  '(fn [state]
     (let [trophy (state :current-trophy)]
       (get [2 3 4 5 6 7 8 9 10 11 12 13 1] (dec trophy)))))
