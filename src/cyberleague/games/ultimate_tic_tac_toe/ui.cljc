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

(defn match-results-view
  [match]
  #?(:cljs
     (let [current-move (r/atom (count (match :moves)))]
       (fn [match]
         (let [moves (match :moves)
               displayed-moves (take @current-move moves)
               p1-color "rgb(0,0,255)"
               p2-color "rgb(255,0,0)"
               p1-color-bg "rgba(0,0,255,0.1)"
               p2-color-bg "rgba(255,0,0,0.1)"
               [p1-moves p2-moves] (->> displayed-moves
                                        (map #(get % "move"))
                                        (partition 2 2 nil)
                                        ((juxt (partial map first) (partial map second)))
                                        (map set))
               mini-board (fn [board-index]
                            (->> (range 9)
                                 (mapv (fn [index]
                                         (cond
                                           (contains? p1-moves [board-index index])
                                           :p1
                                           (contains? p2-moves [board-index index])
                                           :p2
                                           :else
                                           nil)))))]
           [:div
            [:table.results.ultimate-tic-tac-toe
             (let [meta-board (->> (range 9)
                                   (mapv (fn [index]
                                           (get-winner (mini-board index)))))
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
                                                          :p1 p1-color-bg
                                                          :p2 p2-color-bg)
                                                        "transparent")}
                                                     (->border-styles board-index "black"))}
                                  [:table.subboard
                                   (let [board (mini-board board-index)
                                         winning-cells (winning-indexes board)
                                         winner (get-winner board)]
                                     [:tbody
                                      (into [:<>]
                                            (for [sub-row (partition 3 (range 9))]
                                              [:tr
                                               (into [:<>]
                                                     (for [subboard-index sub-row]
                                                       (let [play (condp contains? [board-index subboard-index]
                                                                    p1-moves :p1
                                                                    p2-moves :p2
                                                                    nil)
                                                             winning-cell? (contains? winning-cells subboard-index)]
                                                         [:td {:style (merge {:width "1em"
                                                                              :height "1em"
                                                                              :text-align "center"
                                                                              :color (case play
                                                                                       :p1 p1-color
                                                                                       :p2 p2-color
                                                                                       "transparent")
                                                                              :background (if winning-cell?
                                                                                            (case winner
                                                                                              :p1 p1-color-bg
                                                                                              :p2 p2-color-bg)
                                                                                            "transparent")}
                                                                             (->border-styles subboard-index "#aaa"))}
                                                          (case play
                                                            :p1 "×"
                                                            :p2 "○"
                                                            nil "\u00a0")])))]))])]]))]))])]
            [:div
             (str "Turn " @current-move "/" (count moves))
             [:br]
             [:input {:type "range"
                      :min 0
                      :max (count moves)
                      :step 1
                      :value @current-move
                      :on-change (fn [e]
                                   (reset! current-move (js/parseInt (.. e -target -value) 10)))}]]])))))

(defn >results-styles []
  [:>.results.ultimate-tic-tac-toe

   [:td
    {:border "2px solid black"
     :padding "1em"}]

   [:tr:first-child

    [:td
     {:border-top "none"}]]

   [:tr:last-child

    [:td
     {:border-bottom "none"}]]

   [:tr

    [:td:first-child
     {:border-left "none"}]

    [:td:last-child
     {:border-right "none"}]]

   [:.subboard

    [:td
     {:border "1px solid grey"
      :padding "0.1em"}

     [:&.p1
      {:color "red"}]

     [:&.p2
      {:color colors/blue}]]]])
