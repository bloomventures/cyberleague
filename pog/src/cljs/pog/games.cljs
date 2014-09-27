(ns pog.games)

(defprotocol IGameEngine
  (simultaneous-turns? [_] "Do the players of this game reveal moves simultaneous?")
  (valid-move? [_ move] "Is the player's move syntactically well-formed?")
  (init-state [_ player-1 player-2] "Create the initial state of the game")
  (legal-move? [_ state player move] "Is the player's move for the given game state legal?")
  (next-state [_ state moves] "Calculate the next state, given the current state
                              and an array of player moves like [{:player p1 :move m}]")
  (game-over? [_ state] "Is the game in a finished state?")
  (winner [_ state] "If the game is over, return the winner of the game")

  )

(defmulti make-engine :game/name)

;; Goofspiel
(comment
  ; Example game state
  {:player-cards
   {12345 #{ 1 2 3 13 }
    54321 #{ 1 2 3 13 }}
   :trophy-cards #{ 1 2 3 13 }
   :current-trophy 4
   :history  [ { 12345 1 54321 1 :trophy 1 } … ] }
  ; Example move
  12
  )

(defmethod make-engine "goofspiel"
  [_]
  (reify
    IGameEngine
    (simultaneous-turns? [_] true)

    (valid-move? [_ move] (<= 1 move 13))

    (legal-move? [_ state player move]
      (contains? (get-in state [:player-cards player]) move))

    (next-state [_ {:keys [player-cards trophy-cards current-trophy history] :as state} moves]
      (-> state
          (update-in [:history] conj (apply merge {:trophy (:trophy state)} moves)))
      )

    (game-over? [_ state])

    (winner [_ state]
      )))
