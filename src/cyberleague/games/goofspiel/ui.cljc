(ns cyberleague.games.goofspiel.ui
  (:require
   [reagent.core :as r]
   [cyberleague.client.ui.colors :as colors]))

(defn move-view [_]
  #?(:cljs
     (let [show-log? (r/atom false)]
       (fn [move {:keys [p1-id p2-id]}]
         [:tbody
          [:tr {:on-click (fn [_] (swap! show-log? not))}
           [:td (move :trophy)]
           [:td {:class (when (> (move p1-id) (move p2-id)) "winner")} (move p1-id)]
           [:td {:class (when (< (move p1-id) (move p2-id)) "winner")} (move p2-id)]
           [:td (if @show-log? "×" "▾")]]
          [:tr.log {:class (if @show-log? "show" "hide")}
           [:td {:colSpan 4} "console logging TODO"]]]))))

(defn match-results-view
  [{:match/keys [bots winner] :as match} states moves]
  (let [[p1-id p2-id] (map :bot/id bots)]
    [:div.results.goofspiel
     [:table
      [:thead
       [:tr nil
        [:th "Trophy"]
        [:th (:bot/name (first bots))]
        [:th (:bot/name (second bots))]
        [:th]]]
      [:tfoot
       [:tr
        [:td "Score"]
        [:td {:class (when (= p1-id winner) "winner")}
         (->> moves
              (map (fn [move] (if (> (move p1-id) (move p2-id)) (move :trophy) 0)))
              (apply +))]
        [:td {:class (when (= p2-id winner) "winner")}
         (->> moves
              (map (fn [move] (if (< (move p1-id) (move p2-id)) (move :trophy) 0)))
              (apply +))]
        [:td]]]
      (into [:<>]
            (for [move moves]
              [move-view move {:p1-id p1-id :p2-id p2-id}]))]]))

(defn >results-styles []
  [:>.results.goofspiel

   [:table
    {:width "100%"}

    [:tfoot
     [:td
      {:border-top [["2px" "solid" colors/blue]]}]]

    [:tr
     {:cursor "pointer"}]

    [:th
     {:text-align "center"}]

    [:td
     {:text-align "center"}

     [:&.winner
      {:font-weight 800
       :color colors/blue
       :background colors/blue-ultralight}]]]

   [:.hide
    {:display "none"}]

   [:.log
    {:background "#ccc"
     :font-family "Inconsolata, monospace"
     :text-align "left"}]])
