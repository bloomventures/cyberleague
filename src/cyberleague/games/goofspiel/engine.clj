(ns cyberleague.games.goofspiel.engine
  (:require
   [clojure.set :as set]
   [cyberleague.games.protocol :as protocol]))

(comment
  ; Example game state
  {"player-cards"
   {12345 #{1 2 3 13}
    54321 #{1 2 3 13}}
   "trophy-cards" #{1 2 3 13}
   "current-trophy" 4
   "history"  [{12345 1 54321 1 "trophy" 1} …]}
  ; Example move
  12)

(defn set-next-trophy
  "Helper function for setting the next trophy"
  [{:strs [trophy-cards] :as state}]
  (if (empty? trophy-cards)
    (dissoc state "current-trophy")
    (let [next-trophy (rand-nth (vec trophy-cards))]
      (-> state
          (update-in ["trophy-cards"] disj next-trophy)
          (assoc "current-trophy" next-trophy)))))

(defmethod protocol/make-engine "goofspiel"
  [_]
  (reify
    protocol/IGameEngine
    (simultaneous-turns? [_] true)

    (number-of-players [_] 2)

    (valid-move? [_ move] (<= 1 move 13))

    (init-state [_ players]
      (set-next-trophy
       {"player-cards" (reduce (fn [a pl-id] (assoc a pl-id (set (range 1 14))))
                               {} players)
        "trophy-cards" (set (range 1 14))
        "current-trophy" nil
        "history" []}))

    (anonymize-state-for [_ player-id state]
      (let [other (-> (state "player-cards")
                      keys set (disj player-id) first)]
        (-> state
            (update-in ["player-cards"]
                       set/rename-keys {other "opponent"
                                        player-id "me"})
            (update-in ["history"]
                       (partial map #(set/rename-keys % {other "opponent"
                                                         player-id "me"}))))))

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
