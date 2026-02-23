(ns cyberleague.games.goofspiel.ui
  (:require
   [reagent.core :as r]
   [cyberleague.client.ui.colors :as colors]))

(defn winner-tw [winner?]
  (when winner? "font-extrabold text-#3f51b5 bg-#d5daef"))

(defn move-view [move {:keys [p1-id p2-id]}]
  [:tbody
   [:tr {:tw "cursor-pointer"}
    [:td {:tw "text-center p-1"} (move :trophy)]
    [:td {:tw (str "text-center p-1 " (winner-tw (> (move p1-id) (move p2-id))))} (move p1-id)]
    [:td {:tw (str "text-center p-1 " (winner-tw (< (move p1-id) (move p2-id))))} (move p2-id)]]])

(defn match-results-view
  [{:match/keys [bots winner] :as match} states moves]
  (let [[p1-id p2-id] (map :bot/id bots)]
    [:div.results.goofspiel
     [:table
      {:tw "w-full"}
      [:thead
       [:tr {:tw "cursor-pointer"}
        [:th {:tw "text-center p-1"} "Trophy"]
        [:th {:tw "text-center p-1"} (:bot/name (first bots))]
        [:th {:tw "text-center p-1"} (:bot/name (second bots))]
        [:th]]]
      [:tfoot
       [:tr
        [:td {:tw "text-center p-1"
              :style {:border-top (str "2px solid " colors/blue)}}
         "Score"]
        [:td {:tw (str "text-center p-1 " (winner-tw (= p1-id winner)))
              :style {:border-top (str "2px solid " colors/blue)}}
         (->> moves
              (map (fn [move] (if (> (move p1-id) (move p2-id)) (move :trophy) 0)))
              (apply +))]
        [:td {:tw (str "text-center p-1 " (winner-tw (= p2-id winner)))
              :style {:border-top (str "2px solid " colors/blue)}}
         (->> moves
              (map (fn [move] (if (< (move p1-id) (move p2-id)) (move :trophy) 0)))
              (apply +))]
        [:td {:style {:border-top (str "2px solid " colors/blue)}}]]]
      (into [:<>]
            (for [move moves]
              [move-view move {:p1-id p1-id :p2-id p2-id}]))]]))

