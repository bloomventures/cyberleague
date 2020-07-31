(ns cyberleague.game-registrar)

(def games (atom {}))

(def schema
  [:map
   [:game.config/name string?]
   [:game.config/match-results-view fn?
    ;; TODO fn that takes a match
    ]
   [:game.config/seed-game
    [:map
     [:game.config/name string?]
     [:game.config/description string?]
     [:game.config/rules string?]]]
   [:game.config/seed-bots
    [:vector
     [:map
      [:bot/user-name string?]
      [:bot/game-name string?]
      [:bot/code string?]]]]])

(defn register-game! [game]
  ;; TODO validate game follows a schema
  (swap! games assoc (game :game.config/name) game))
