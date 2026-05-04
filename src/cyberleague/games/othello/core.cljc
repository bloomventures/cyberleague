(ns cyberleague.games.othello.core
  (:require
   [cyberleague.common.schema :as s]
   [cyberleague.common.uuid :as uuid]
   [cyberleague.game-registrar]
   [cyberleague.games.othello.bots :as bots]
   [cyberleague.games.othello.engine] ;; so it gets registered
   [cyberleague.games.othello.helpers]
   [cyberleague.games.othello.ui :as ui]))

(def Move
  [:int {:min 0 :max 63}])

#_(malli.core/validate Move 63)

(def Context
  [:map
   [:board
    [:vector {:min 64 :max 64}
     [:enum "B" "W" "E"]]]
   [:history
    [:vector
     [:map
      [:player s/BotId]
      [:move Move]]]]
   [:available-moves
    [:vector
     [:move Move]]]])

#_(malli.core/validate [:vector {:length 4} :int] [1 2 2 2 5 6])

(cyberleague.game-registrar/register-game!
 {:game.config/name
  "othello"

  :game.config/slug
  "othello"

  :game.config/description
  "Othello is a two-player strategy game played on an 8x8 board, where players compete to flip their opponent's discs by surrounding them."

  :game.config/rules
  (str
   "- The board starts with two black and two white discs in the center. Black moves first.\n"
   "- On each turn, a player must place a disc that flips at least one of the opponent's discs.\n"
   "- A flip occurs when the newly placed disc and an existing disc of the same color sandwich one or more of the opponent's discs in a straight line (horizontally, vertically, or diagonally).\n"
   "- All sandwiched opponent discs are flipped to the placing player's color. A single move can flip in multiple directions at once.\n"
   "- The game ends when the current player has no legal moves.\n"
   "- The player with the most discs of their color on the board wins. A draw is possible.")

  :game.config/state-spec
  Context

  :game.config/technical-notes
  "Moves represent the cell index in row-major order (0=top-left, 63=bottom-right)."

  :game.config/context-spec
  Context

  :game.config/context-example
  {:board ["E" "E" "E" "E" "E" "E" "E" "E"
           "E" "E" "E" "E" "E" "E" "E" "E"
           "E" "E" "E" "E" "W" "E" "E" "E"
           "E" "E" "B" "B" "W" "E" "E" "E"
           "E" "E" "E" "B" "B" "E" "E" "E"
           "E" "E" "E" "E" "E" "B" "E" "E"
           "E" "E" "E" "E" "E" "E" "E" "E"
           "E" "E" "E" "E" "E" "E" "E" "E"]
   :history [{:player (uuid/from-string "player-1")
              :move 26}
             {:player (uuid/from-string "player-2")
              :move 20}
             {:player (uuid/from-string "player-1")
              :move 45}]
   :current-turn "W"
   :available-moves [34 44 25 42]
   :marker {(uuid/from-string "player-1") "B"
            (uuid/from-string "player-2") "W"}}

  :game.config/move-spec
  Move

  :game.config/move-example
  32

  :game.config/match-results-view
  ui/match-results-view

  :game.config/test-bot
  {:blueprint/env-slug "clojure-sci"
   :blueprint/code (pr-str bots/random-valid-bot)}

  :game.config/seed-bots
  [{:blueprint/env-slug "clojure-sci"
    :blueprint/code (pr-str bots/random-valid-bot)}
   {:blueprint/env-slug "clojure-sci"
    :blueprint/code (pr-str bots/first-valid-bot)}]})


