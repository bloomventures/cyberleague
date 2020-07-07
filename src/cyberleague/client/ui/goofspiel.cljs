(ns cyberleague.client.ui.goofspiel
  (:require
    [reagent.core :as r]))

(defn move-view [_]
  (let [show-log? (r/atom false)]
    (fn [move {:keys [p1-id p2-id]}]
      [:tbody
       [:tr.clickable {:on-click (fn [_] (swap! show-log? not))}
        [:td (move "trophy")]
        [:td {:class (when (> (move p1-id) (move p2-id)) "winner")} (move p1-id)]
        [:td {:class (when (< (move p1-id) (move p2-id)) "winner")} (move p2-id)]
        [:td (if @show-log? "×" "▾")]]
       [:tr.log {:class (if @show-log? "show" "hide")}
        [:td {:colSpan 4} "console logging TODO"]]])))

(defn match-results-view
  [{:keys [bots winner] :as match}]
  (let [p1-id (:id (first bots))
        p2-id (:id (second bots))]
    [:table.results.goofspiel
     [:thead
      [:tr nil
       [:th "Trophy"]
       [:th (:name (first bots))]
       [:th (:name (second bots))]
       [:th]]]
     [:tfoot
      [:tr
       [:td "Score"]
       [:td {:class (when (= p1-id winner) "winner")}
        (->> match
             :moves
             (map (fn [move] (if (> (move p1-id) (move p2-id)) (move "trophy") 0)))
             (apply +))]
       [:td {:class (when (= p2-id winner) "winner")}
        (->> match
             :moves
             (map (fn [move] (if (< (move p1-id) (move p2-id)) (move "trophy") 0)))
             (apply +))]
       [:td]]]
     (into [:<>]
            (for [move (:moves match)]
              [move-view move {:p1-id p1-id :p2-id p2-id}]))]))
