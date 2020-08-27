(ns cyberleague.games.othello.ui
  (:require
    [reagent.core :as r]
    [cyberleague.client.ui.colors :as colors]))

(defn get-stone-count
  [stone-color board]
  (->> board
       (filter (fn [square]
                 (= square stone-color)))
       count))

(defn match-results-view*
  [match states _]
  (let [state (last states)
        bots-by-id (->> (match :match/bots)
                        (reduce (fn [memo bot]
                                  (assoc memo (bot :bot/id) bot)) {}))]
    [:div
     {:style
      {:font-size 26}}
     [:table
      [:tbody
       (for [[bot-id marker] (state :marker)]
         ^{:key bot-id}
         [:tr
          [:td (:bot/name (bots-by-id bot-id))]
          [:td (case marker
                 "B" "⚫️"
                 "W" "⚪️")]])]]

     [:table.results.othello
      (let [meta-board (state :board)
            move (:move (last (get-in state [:history])))]
        [:tbody
         (into [:<>]
               (for [row (partition 8 (range 64))]
                 [:tr
                  (into [:<>]
                        (for [board-index row]

                          [:td {:style
                                (merge {:width "1em"
                                        :height "1em"
                                        :text-align "center"
                                        :background (if (= board-index move) "lightcoral" "lightgreen")
                                        :color "black"
                                        :border "1px solid grey"})} (condp = (meta-board board-index)
                                                                      "B" "⚫️"
                                                                      "W" "⚪️"
                                                                      "\u00a0")]))]))])]

      [:table
       [:tbody
        [:tr
         [:td "⚫️ = " [get-stone-count "B" (:board state)]]
         [:td "⚪️  = " [get-stone-count "W" (:board state)]]]]]]))

(defn match-results-view
  [match state move]
  [match-results-view* match state move])

(defn >results-styles [])


