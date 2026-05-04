(ns cyberleague.games.ultimate-tic-tac-toe.core
  (:require
   [cyberleague.common.schema :as s]
   [cyberleague.common.uuid :as uuid]
   [cyberleague.game-registrar]
   [cyberleague.games.ultimate-tic-tac-toe.bots :as bots]
   [cyberleague.games.ultimate-tic-tac-toe.engine] ;; so it gets registered
   [cyberleague.games.ultimate-tic-tac-toe.ui :as ui]))

(def Move
  [:tuple
   [:int {:min 0 :max 8}]
   [:int {:min 0 :max 8}]])

#_(malli.core/validate Move [0 1])

(def Context
  [:map
   [:grid
    [:vector {:min 9 :max 9}
     [:vector {:min 9 :max 9}
      [:enum "x" "o" nil]]]]
   [:history
    [:vector
     [:map
      [:player s/BotId]
      [:move Move]]]]])

(cyberleague.game-registrar/register-game!
 {:game.config/name
  "ultimate tic-tac-toe"

  :game.config/slug
  "ultimate-tic-tac-toe"

  :game.config/description
  (str
   "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master its bigger brother.\n"
   "In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested inside a meta Tic-Tac-Toe game.")

  :game.config/rules
  (str
   "- The board is a 3×3 grid of 9 sub-boards, each a 3×3 Tic-Tac-Toe board.\n"
   "- The first move may be played anywhere.\n"
   "- On each subsequent move, the current player must play in the sub-board whose index matches the cell their opponent just played.\n"
   "- If that sub-board is already decided (won or drawn), the current player may play in any undecided sub-board.\n"
   "- A sub-board is won by classic Tic-Tac-Toe rules (three marks in a row; horizontally, vertically, or diagonally).\n"
   "- The game is won by the player who wins three sub-boards in a row on the meta board.\n"
   "- The game may end in a draw if all sub-boards are decided without a winner.")

  :game.config/state-spec
  Context

  :game.config/technical-notes
  "Moves are [board cell], where both are indices 0–8 in row-major order (0=top-left, 8=bottom-right)."

  :game.config/context-spec
  Context

  :game.config/context-example
  {:grid [["x" "o" nil nil nil nil nil nil nil]
          (vec (repeat 9 nil))
          (vec (repeat 9 nil))
          (vec (repeat 9 nil))
          (vec (repeat 9 nil))
          (vec (repeat 9 nil))
          (vec (repeat 9 nil))
          (vec (repeat 9 nil))
          (vec (repeat 9 nil))]
   :history [{:player (uuid/from-string "player-1")
              :move [0 0]}
             {:player (uuid/from-string "player-2")
              :move [0 1]}]}

  :game.config/move-spec
  Move

  :game.config/move-example
  [0 1]

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
