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

(def js-random-bot
  (str
    "function(state) {
      function getRandomInt(max) {
        return Math.floor(Math.random() * Math.floor(max));
      }
      const myCards = state['player-cards']['me'];
      return myCards[getRandomInt(myCards.length - 1)];
    }"))
