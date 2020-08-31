(ns cyberleague.games.othello.helpers)

(def other-stone {"B" "W"
                  "W" "B"})

(def initial-board (-> (vec (repeat 64 "E"))
                       (assoc 27 "W")
                       (assoc 28 "B")
                       (assoc 35 "B")
                       (assoc 36 "W")))

(def adjacent-squares
  [(fn up [x]
     (if (> x 7)
       (- x 8) nil))
   (fn down [x]
     (if (< x 56)
       (+ x 8) nil))
   (fn left [x]
     (if (not= (mod x 8) 0)
       (dec x) nil))
   (fn right [x]
     (if (not= (mod (inc x) 8) 0)
       (inc x) nil))
   (fn up-left [x]
     (if (not (or (< x 8) (= (mod x 8) 0)))
       (- x 9) nil))
   (fn up-right [x]
     (if (not (or (< x 8) (= (mod (inc x) 8) 0)))
       (- x 7) nil))
   (fn down-left [x]
     (if (not (or (> x 55) (= (mod x 8) 0)))
       (+ x 7) nil))
   (fn down-right [x]
     (if (not (or (> x 55) (= (mod (inc x) 8) 0)))
       (+ x 9) nil))])

(defn neighbouring-squares
  [move]
  (->> adjacent-squares
       (map (fn [f]
              [f (f move)]))
       (remove (fn [[_ square]] (nil? square)))))

(defn get-squares-of-stone
  [board stone]
  (->> (map-indexed vector board)
       (filter (fn [[_ v]]
                 (= v stone)))
       (map first)))

(defn validated-move
  [board [direction-fn square] stone target]
  (let [next-square (direction-fn square)]
    (when (some? next-square)
      (condp = (board next-square)
        target next-square
        stone (recur board [direction-fn next-square] stone target)
        nil))))

(defn neighbouring-occupied-squares
  [board stone squares]
  (->> squares
       (filter (fn [[_ sq]]
                 (= (board sq) (other-stone stone))))))

(defn is-valid-move?
  [board square stone]
  (when (= (board square) stone)
    (->> square
         (neighbouring-squares)
         (neighbouring-occupied-squares board stone)
         (map (fn [curr]
                (validated-move board curr (other-stone stone) "E")))
         (filter some?))))

(defn valid-moves
  [board stone]
  (->> (get-squares-of-stone board stone)
       (reduce (fn [acc curr]
                 (let [new-move (is-valid-move? board curr stone)]
                   (if (seq new-move)
                     (into acc new-move)
                     acc))) [])))

(defn squares-in-direction
  [direction-fn square]
  (->> (repeat square)
       (reduce (fn [acc _]
                 (let [next-square (direction-fn (last acc))]
                   (if (some? next-square) (conj acc next-square) (reduced acc)))) [square])))

(defn make-changes-to-board
  [board stone move squares]
  (->> squares
       (reduce (fn [new-board square]
                 (assoc new-board square stone)) (assoc board move stone))))

(defn squares-to-change
  [board stone valid-squares]
  (->> valid-squares
       (map (fn [[direction-fn square]]
              (squares-in-direction direction-fn square)))
       (mapcat (fn [squares]
                 (take-while (fn [curr]
                               (not= (board curr) stone)) squares)))))


(defn valid-directions
  [old-board stone potential-moves]
  (->> potential-moves
       (filter (fn [curr]
                 (validated-move old-board curr (other-stone stone) stone)))))

(defn apply-move
  [current-board stone move]
  (->> move
       (neighbouring-squares)
       (neighbouring-occupied-squares current-board stone)
       (valid-directions current-board stone)
       (squares-to-change current-board stone)
       (make-changes-to-board current-board stone move)))

(defn squares-to-highlight
  [board stone move]
  (when board (->> move
       (neighbouring-squares)
       (neighbouring-occupied-squares board stone)
       (valid-directions board stone)
       (squares-to-change board stone)
       set)))

(def test-board
  (-> initial-board
      (assoc 43 "W")
      (assoc 42 "B")))

(defn stone-count
  [board stone]
  (->> board
       (filter (fn [square]
                 (= square stone)))
       count))
