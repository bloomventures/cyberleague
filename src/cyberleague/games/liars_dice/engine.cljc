(ns cyberleague.games.liars-dice.engine
  (:require
   [cyberleague.games.protocol :as protocol]))

(comment
  ;; Example game state (in progress)
  {:dice {0 [3 1 4 2 5]
          1 [2 2 6 1 3]}
   :history [{:player-id 0 :move {:action "bid" :quantity 2 :face 3}}
             {:player-id 1 :move {:action "bid" :quantity 3 :face 4}}]}

  ;; Example game state (finished)
  {:dice {0 [3 1 4 2 5]
          1 [2 2 6 1 3]}
   :history [{:player-id 0 :move {:action "bid" :quantity 2 :face 3}}
             {:player-id 1 :move {:action "bid" :quantity 3 :face 4}}
             {:player-id 0 :move {:action "challenge"}}]}

  ;; Example moves
  {:action "bid" :quantity 3 :face 4}
  {:action "challenge"})

(defn roll-dice [n]
  (vec (repeatedly n #(inc (rand-int 6)))))

(defn count-matching
  "Count dice showing `face` or showing 1 (wild)."
  [hands face]
  (->> (vals hands)
       (apply concat)
       (filter #(or (= face %) (= 1 %)))
       count))

(defn other-player [players player]
  (->> players (remove #(= player %)) first))

(defn active-player [players history]
  (if (empty? history)
    (first players)
    (other-player players (:player-id (last history)))))

(defn last-bid [history]
  (->> history (filter #(= "bid" (-> % :move :action))) last))

(defn higher-bid?
  "A new bid is valid if it raises quantity (any face) or raises face at same quantity."
  [current-bid {:keys [quantity face]}]
  (or (nil? current-bid)
      (> quantity (-> current-bid :move :quantity))
      (and (= quantity (-> current-bid :move :quantity))
           (> face (-> current-bid :move :face)))))

(defn winner [state]
  (when (= "challenge" (-> state :history last :move :action))
    (let [history (:history state)
          challenger (:player-id (last history))
          bid (last-bid history)
          bid-valid? (>= (count-matching (:dice state) (-> bid :move :face))
                         (-> bid :move :quantity))]
      (if bid-valid? (:player-id bid) challenger))))

(defmethod protocol/make-engine "liars-dice"
  [_]
  (reify
    protocol/IGameEngine

    (simultaneous-turns? [_] false)

    (number-of-players [_] 2)

    (valid-move? [_ move]
      (and (map? move)
           (contains? #{"bid" "challenge"} (:action move))
           (if (= "bid" (:action move))
             (and (int? (:quantity move))
                  (>= (:quantity move) 1)
                  (int? (:face move))
                  (<= 1 (:face move) 6))
             true)))

    (init-state [_ players]
      {:dice (into {} (map (fn [p] [p (roll-dice 5)]) players))
       :history []})

    (anonymize-state-for [_ player-id state]
      {:my-id player-id
       :my-dice (get-in state [:dice player-id])
       :history (:history state)})

    (legal-move? [_ state player move]
      (let [players (keys (:dice state))
            history (:history state)]
        (and (= player (active-player players history))
             (case (:action move)
               "challenge" (some? (last-bid history))
               "bid" (higher-bid? (last-bid history) move)
               false))))

    (next-state [_ state moves]
      (let [[player move] (first moves)]
        (update state :history conj {:player-id player :move move})))

    (game-over? [_ state]
      (= "challenge" (-> state :history last :move :action)))

    (winner [_ state]
       (winner state))))
