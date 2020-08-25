(ns cyberleague.games.othello.engine
  (:require
   [cyberleague.games.protocol :as protocol]
   [cyberleague.games.othello.helpers :as othello]))

(comment
  ; Example game state
  {:board ["E" "E" "E" "B" "W" "..."]
   "..." (comment "64 squares in total, 8 squares per row")
   :history [{:player 1234 :move 33}
             {:player 54321 :move 34}]
   :available-moves [34 45 23]
   :marker {1234 "B"
            54321 "W"}})

(defmethod protocol/make-engine "othello"
  [_]
  (reify
    protocol/IGameEngine
    (simultaneous-turns? [_] false)

    (number-of-players [_] 2)

    (init-state [_ players]
      {:board othello/initial-board
       :history []
       :current-turn "B"
       :available-moves (othello/valid-moves othello/initial-board "B")
       :marker {(first players) "B"
                (second players) "W"}})

    (anonymize-state-for [_ player-id state]
      state)

    (valid-move? [_ move]
      (and (integer? move)
           (>= move 0)
           (< move 64)))

    (legal-move? [_ {:keys [available-moves]} _ move]
      (-> (filter (fn [curr]
                    (= move curr)) available-moves)
          seq))

    (next-state [_ {:keys [board current-turn] :as state} moves]
      (let [[player move-coordinate] (first moves)
            next-move (othello/apply-move board current-turn move-coordinate)
            next-stone (othello/other-stone current-turn)]
        (-> state
            (assoc-in [:board] next-move)
            (update-in [:history] conj {:player player :move move-coordinate})
            (assoc-in [:current-turn] next-stone)
            (assoc-in [:available-moves] (othello/valid-moves next-move next-stone)))))

    (game-over? [_ {:keys [available-moves]}]
      (empty? available-moves))

    (winner [_ {:keys [board]}]
      (case (compare (othello/stone-count board "B")
                     (othello/stone-count board "W"))
        -1 "W"
        0 false
        1 "B"))))
