(ns cyberleague.games.ultimate-tic-tac-toe.bots)

(def random-valid-bot
  '(let [won-subboard (fn [board]
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
         board-decided? (fn [board] (or (won-subboard board) (not-any? nil? board)))]
     (fn [{:keys [history grid] :as state}]
       (if (empty? history)
         ; I'm first player
         [2 2]
         ; otherwise, we need to play in the correct sub-board
         (let [[b sb] (get (last history) :move)
               board-index (if (board-decided? (grid sb))
                             (->> (range 0 9) (remove (comp board-decided? grid)) rand-nth)
                             sb)
               board (grid board-index)]
           [board-index
            (->> (range 0 9)
                 (filter (comp nil? (partial get board)))
                 ;; choose random valid move
                 rand-nth)])))))

(def first-valid-bot
  '(let [won-subboard (fn [board]
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
         board-decided? (fn [board] (or (won-subboard board) (not-any? nil? board)))]
     (fn [{:keys [history grid] :as state}]
       (if (empty? history)
      ; I'm first player
         [2 2]
      ; otherwise, we need to play in the correct sub-board
         (let [[b sb] (get (last history) :move)
               board-index (if (board-decided? (grid sb))
                             (->> (range 0 9) (remove (comp board-decided? grid)) first)
                             sb)
               board (grid board-index)]
           [board-index
            (->> (range 0 9)
                 (filter (comp nil? (partial get board)))
              ;; choose first valid move
                 first)])))))
