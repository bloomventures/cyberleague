(ns cyberleague.games.othello.starter-code
  (:require
   [cyberleague.games.othello.bots :as bots]))

(def starter-code
  {"clojure"
   (pr-str bots/random-valid-bot)})
