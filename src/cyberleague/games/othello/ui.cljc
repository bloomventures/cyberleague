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

(defn marker-view [marker]
  [:span {:style {:display "inline-block"
                  :width "0.5em"
                  :height "0.5em"
                  :border (str "1px solid "
                               (if (= "E" marker)
                                 "transparent"
                                 "black"))
                  :background (case marker
                                "B" "black"
                                "W" "white"
                                "E" "transparent")
                  :border-radius "50%"}}])

(defn match-results-view*
  [match states _]
  (let [state (last states)
        bots-by-id (->> (match :match/bots)
                        (reduce (fn [memo bot]
                                  (assoc memo (bot :bot/id) bot)) {}))]
    [:div
     [:table.results.othello
      {:style
       {:font-size 26}}
      (let [meta-board (state :board)
            move (:move (last (get-in state [:history])))]
        [:tbody
         (into [:<>]
               (for [row (partition 8 (range 64))]
                 [:tr
                  (into [:<>]
                        (for [board-index row]
                          [:td {:style
                                {:text-align "center"
                                 :padding "0.25em"
                                 :background (if (= board-index move) "lightcoral" "#5bbc5b")
                                 :border "1px solid #388a38"}}
                           [marker-view (meta-board board-index)]]))]))])]
     [:div {:style {:display "flex"
                    :justify-content "space-between"}}
      (for [[bot-id marker] (state :marker)]
        ^{:key bot-id}
        [:div
         [:span (:bot/name (bots-by-id bot-id))] " "
         [:span [marker-view marker]] " "
         [:span (get-stone-count marker (:board state))]])]]))

(defn match-results-view
  [match state move]
  [match-results-view* match state move])

(defn >results-styles [])


