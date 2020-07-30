(ns cyberleague.games.ultimate-tic-tac-toe.engine
  (:require
   [clojure.set :as set]
   [cyberleague.games.protocol :as protocol]))

(comment
  ; Example game state
  {"grid" [["x" nil "o"  ... (comment "nine total things")]
           ; repeated 8 more times
           ]
   "history" [{"player" 1234 "move" [0 0]} ; first player placed an x at [0 0]
              {"player" 54321 "move" [0 3]} ; second player places o at [0 3]
              ]
   "marker" {12345 "x"
             54321 "o"}})

(defn won-subboard
  "Return the winner (x or o) of a basic tic-tac-toe game, false no winner"
  [board]
  (let [all-equal (fn [v] (and (apply = v) (first v)))]
    (or
      ; horizontal lines
     (all-equal (subvec board 0 3))
     (all-equal (subvec board 3 6))
     (all-equal (subvec board 6 9))
      ; vertical lines
     (all-equal (vals (select-keys board [0 3 6])))
     (all-equal (vals (select-keys board [1 4 7])))
     (all-equal (vals (select-keys board [2 5 8])))
      ; diagonals
     (all-equal (vals (select-keys board [0 4 8])))
     (all-equal (vals (select-keys board [2 4 6]))))))

(defn board-decided?
  "Has the given board either been won or played to a draw?"
  [board]
  (or (won-subboard board) (not-any? nil? board)))

(defmethod protocol/make-engine "ultimate tic-tac-toe"
  [_]
  (reify
    protocol/IGameEngine
    (simultaneous-turns? [_] false)

    (number-of-players [_] 2)

    (init-state [_ players]
      {"grid" (vec (repeat 9 (vec (repeat 9 nil))))
       "history" []
       "marker" {(first players) "x"
                 (second players) "o"}})

    (anonymize-state-for [_ player-id state]
      state)

    (valid-move? [_ move]
      (and (coll? move)
           (= 2 (count move))
           (every? #(<= 0 % 8) move)))

    (legal-move? [_ {:strs [history grid] :as state} player move]
      (or (empty? history)
          (and
           (nil? (get-in grid move)) ; the space is currently unoccupied and....
           (not (board-decided? (get-in state ["grid" (first move)]))) ; the grid is undecided
           (let [[_ subboard] ((last history) "move")]
             (or (board-decided? (get-in state ["grid" subboard]))
                 (= subboard (first move)))))))

    (next-state [_ state move]
      (let [[player] (keys move)
            [loc] (vals move)]
        (-> state
            (assoc-in (cons "grid" loc) (get-in state ["marker" player]))
            (update-in ["history"] conj {"player" player "move" loc}))))

    (winner [_ state]
      (->> (mapv #(won-subboard (get-in state ["grid" %])) (range 9))
           won-subboard
           (get (set/map-invert (state "marker")))))

    (game-over? [this state]
      (or (every? board-decided? (state "grid"))
          (not (nil? (protocol/winner this state)))))))
