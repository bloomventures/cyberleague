(ns cyberleague.games)

(defprotocol IGameEngine
  (simultaneous-turns? [_] "Do the players of this game reveal moves simultaneous?")
  (number-of-players [_] "Return the number of players in the game")
  (valid-move? [_ move] "Is the player's move syntactically well-formed?")
  (init-state [_ players] "Create the initial state of the game")
  (legal-move? [_ state player move] "Is the player's move for the given game state legal?")
  (next-state [_ state moves] "Calculate the next state, given the current state
                              and an map of player moves like {player-1-id move1 player-2-id move2}")
  (game-over? [_ state] "Is the game in a finished state?")
  (winner [_ state] "If the game is over, return the winner of the game"))

(defmulti make-engine :game/name)

;; Goofspiel
(comment
  ; Example game state
  {"player-cards"
   {12345 #{ 1 2 3 13 }
    54321 #{ 1 2 3 13 }}
   "trophy-cards" #{ 1 2 3 13 }
   "current-trophy" 4
   "history"  [ { 12345 1 54321 1 "trophy" 1 } … ] }
  ; Example move
  12
  )

(defn set-next-trophy
  "Helper function for setting the next tropyh"
  [{:strs [trophy-cards] :as state}]
  (if (empty? trophy-cards)
    (dissoc state "current-trophy")
    (let [next-trophy (rand-nth (vec trophy-cards))]
      (-> state
          (update-in ["trophy-cards"] disj next-trophy)
          (assoc "current-trophy" next-trophy)))))

(defmethod make-engine "goofspiel"
  [_]
  (reify
    IGameEngine
    (simultaneous-turns? [_] true)

    (number-of-players [_] 2)

    (valid-move? [_ move] (<= 1 move 13))

    (init-state [_ players]
      (set-next-trophy
        {"player-cards" (reduce (fn [a pl-id] (assoc a pl-id (set (range 1 14))))
                               {} players)
         "trophy-cards" (set (range 1 14))
         "current-trophy" nil
         "history" [ ]}))

    (legal-move? [_ state player move]
      (contains? (get-in state ["player-cards" player]) move))

    (next-state [_ {:strs [player-cards trophy-cards current-trophy history] :as state} moves]
      (-> (reduce-kv (fn [st player move] (update-in st ["player-cards" player] disj move))
                     state moves)
          (update-in ["history"] conj (merge {"trophy" current-trophy} moves))
          set-next-trophy))

    (game-over? [_ state]
      (and (empty? (state "trophy-cards")) (nil? (state "current-trophy"))))

    (winner [_ state]
      (let [scores (reduce (fn [sc mv]
                             (let [trophy (mv "trophy")
                                   mv (dissoc mv "trophy")
                                   highest (reduce max 0 (vals mv))
                                   winner (filter (comp (partial = highest) second) mv)]
                               (if (= 1 (count winner))
                                 (update-in sc [(ffirst winner)] (fnil (partial + trophy) 0))
                                 sc)))
                           {} (state "history"))
            high-score (reduce max 0 (vals scores))
            winner (filter (comp (partial = high-score) second) scores)]
        (when (= 1 (count winner))
          (ffirst winner))))))

;; Ultimate Tic-Tac-Toe
(comment
  ; Example game state
  {
   "grid" [  [ "x" nil "o"  ... (comment "nine total things")]
           ; repeated 8 more times
           ]
   "history" [ { "player" 1234 "move" [0 0] } ; first player placed an x at [0 0]
               { "player" 54321 "move" [0 3 ] } ; second player places o at [0 3]
              ]
  }
  )
(defmethod make-engine "ultimate tic-tac-toe"
  [_]
  (reify
    IGameEngine
    (simultaneous-turns? [_] false)

    (number-of-players [_] 2)

    (init-state [_ players]
      {"grid" (vec (repeat 9 (vec (repeat 9 nil))))
       "history" []
       (first players) "x"
       (second players) "o"})

    (valid-move? [_ move]
      (and (coll? move)
        (= 2 (count move))
        (every? #(<= 0 % 8) move)))

    (legal-move? [_ {:strs [history grid] :as state} player move]
      ; move must be to sub-board corresponding to the location in the
      ; subboard of the previous move, unless that board is full, in which
      ; case the player can now play anywhere that hasn't already been played
      (let [[_ subboard] ((last history) "move")]
        (if (some nil? (get grid subboard))
          (and (= subboard (first move)) (nil? (get-in grid move)))
          (nil? (get-in grid move)))))

    (next-state [_ state move]
      (let [[player] (keys move)
            [loc] (vals move)]
        (assoc-in state (cons "grid" loc) (state player))))

    (game-over? [_ state]
      )

    (winner [_ state]
      )))
