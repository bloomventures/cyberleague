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

(defn set-next-trophy
  "Helper function for setting the next tropyh"
  [{:keys [trophy-cards] :as state}]
  (let [next-trophy (rand-nth (vec trophy-cards))]
    (-> state
        (update-in [:trophy-cards] disj next-trophy)
        (assoc :current-trophy next-trophy))))

(defmethod make-engine "goofspiel"
  [_]
  (reify
    IGameEngine
    (simultaneous-turns? [_] true)

    (valid-move? [_ move] (<= 1 move 13))

    (init-state [_ player-1 player-2]
      (set-next-trophy
        {:player-cards
         {player-1 (set (range 1 14))
          player-2 (set (range 1 14))}
         :trophy-cards (set (range 1 14))
         :current-trophy nil
         :history [ ]}))

    (legal-move? [_ state player move]
      (contains? (get-in state [:player-cards player]) move))

    (next-state [_ {:keys [player-cards trophy-cards current-trophy history] :as state} moves]
      (let [next-trophy (rand-nth (vec trophy-cards))]
        (-> state
            (update-in [:history] conj (apply merge {:trophy current-trophy} moves))
            set-next-trophy)))

    (game-over? [_ state])

    (winner [_ state]
      )))
