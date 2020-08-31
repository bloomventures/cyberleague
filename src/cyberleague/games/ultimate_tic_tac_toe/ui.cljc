(ns cyberleague.games.ultimate-tic-tac-toe.ui
  (:require
   [reagent.core :as r]
   [cyberleague.client.ui.colors :as colors]))

(def chains
  [[0 3 6]
   [1 4 7]
   [2 5 8]
   [0 1 2]
   [3 4 5]
   [6 7 8]
   [0 4 8]
   [2 4 6]])

(defn winning-indexes [board]
  (->> chains
       (mapv (fn [chain]
               (mapv
                (fn [index]
                  [index
                   (get board index)]) chain)))
       (filter (fn [chain]
                 (let [chain (map second chain)]
                   (and
                    (some? (first chain))
                    (apply = chain)))))
       first ;; or we want to show all?
       (map first)
       set))

(defn get-winner [board]
  (->> chains
       (mapv (fn [chain]
               (mapv
                (fn [index]
                  (get board index)) chain)))
       (filter (fn [chain]
                 (and
                  (some? (first chain))
                  (apply = chain))))
       first
       first))

(defn ->border-styles [index color]
  (merge
   (when (#{3 4 5} index)
     {:border-top (str "1px solid " color)
      :border-bottom (str "1px solid " color)})
   (when (#{1 4 7} index)
     {:border-right (str "1px solid " color)
      :border-left (str "1px solid " color)})))


(def p1-color "rgb(0,0,255)")
(def p2-color "rgb(255,0,0)")
(def p1-color-bg "rgba(0,0,255,0.1)")
(def p2-color-bg "rgba(255,0,0,0.1)")

(defn marker-view [marker]
  [:span {:style {:color (case marker
                           "x" p1-color
                           "o" p2-color
                           "transparent")

                  }}
   (case marker
     "x" "×"
     "o" "○"
     nil "\u00a0")])

(defn match-results-view*
  [match states _]
  (let [state (last states)
        bots-by-id (->> (match :match/bots)
                        (reduce (fn [memo bot]
                                  (assoc memo (bot :bot/id) bot)) {}))]
    [:div


     [:table.results.ultimate-tic-tac-toe
      (let [meta-board (->> (state :grid)
                            (mapv get-winner))
            winning-cells (winning-indexes meta-board)
            winner (get-winner meta-board)]
        [:tbody
         (into [:<>]
               (for [row (partition 3 (range 9))]
                 [:tr
                  (into [:<>]
                        (for [board-index row]
                          [:td {:style (merge {:background
                                               (if (contains? winning-cells board-index)
                                                 (case winner
                                                   "x" p1-color-bg
                                                   "o" p2-color-bg)
                                                 "transparent")}
                                              (->border-styles board-index "black"))}
                           [:table.subboard
                            (let [board (get-in state [:grid board-index])
                                  winning-cells (winning-indexes board)
                                  winner (get-winner board)]
                              [:tbody
                               (into [:<>]
                                     (for [sub-row (partition 3 (range 9))]
                                       [:tr
                                        (into [:<>]
                                              (for [subboard-index sub-row]
                                                (let [play (get-in state [:grid board-index subboard-index])
                                                      winning-cell? (contains? winning-cells subboard-index)]
                                                  [:td {:style (merge {:width "1em"
                                                                       :height "1em"
                                                                       :text-align "center"
                                                                       :background (if winning-cell?
                                                                                     (case winner
                                                                                       "x" p1-color-bg
                                                                                       "o" p2-color-bg)
                                                                                     "transparent")}
                                                                      (->border-styles subboard-index "#aaa"))}
                                                   [marker-view play]])))]))])]]))]))])]
     [:div {:style {:display "flex"
                    :justify-content "space-between"}}
      (for [[bot-id marker] (state :marker)]
         ^{:key bot-id}
         [:div
          [:span (:bot/name (bots-by-id bot-id))] " "
          [:span [marker-view marker]]])]]))

;; have an intermediary view, just so it refreshes nicely with reagent + the game registry
(defn match-results-view
  [match state move]
  [match-results-view* match state move])

(defn >results-styles []
  [:>.results.ultimate-tic-tac-toe])
