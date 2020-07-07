(ns cyberleague.client.ui.ultimate-tic-tac-toe
  (:require
    [reagent.core :as r]))

(defn match-results-view
  [match]
  (let [current-move (r/atom (count (match :moves)))]
    (fn [match]
      (let [moves (match :moves)
            displayed-moves (take current-move moves)
            p1 (get (first moves) "player")
            p2 (get (second moves) "player")
            [p1-moves p2-moves] (->> displayed-moves
                                     (map #(get % "move"))
                                     (partition 2 2 nil)
                                     ((juxt (partial map first) (partial map second)))
                                     (map set))]
        [:div
         [:table.results.tic-tac-toe
          [:tbody
           (for [row (partition 3 (range 9))]
             [:tr
              (for [board-idx row]
                [:td
                 [:table.subboard
                  [:tbody
                   (for [sub-row (partition 3 (range 9))]
                     [:tr
                      (for [subboard-idx sub-row]
                        (let [winner (condp contains? [board-idx subboard-idx]
                                       p1-moves :p1
                                       p2-moves :p2
                                       :no)]
                          [:td {:class (name winner)}
                           (case winner
                             :p1 "X"
                             :p2 "O"
                             :no ".")]))])]]])])]]
         [:div
          (str "Turn " current-move "/" (count moves))
          [:br]
          [:input {:type "range"
                   :min 0
                   :max (count moves)
                   :step 1
                   :value current-move
                   :on-change (fn [e] (reset! current-move (.. e -target -value)))}]]]))))
