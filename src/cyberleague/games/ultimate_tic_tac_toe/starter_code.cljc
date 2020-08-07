(ns cyberleague.games.ultimate-tic-tac-toe.starter-code
  (:require
   [cyberleague.games.ultimate-tic-tac-toe.bots :as bots]))

(def starter-code
  {"clojure"
   (pr-str bots/random-valid-bot)

   "javascript"
   "function(state) {
      // TODO
      return [2 2];
    }"})
