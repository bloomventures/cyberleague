(ns cyberleague.client.ui.match-card
  (:require
    [cyberleague.client.state :as state]
    [cyberleague.client.ui.match-results :refer [match-results-view]]))

(defn match-card-view
  [{:keys [data] :as card}]
  [:div.card.match
   [:header "MATCH"
    [:a {:on-click (fn [_] (state/nav! :game (:id (:game data))))} (str "#" (:name (:game data)))]
    [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
   [:div.content
    [:h1
     [:a {:on-click (fn [_] (state/nav! :bot (:id (first (:bots data)))))} (:name (first (:bots data)))]
     " vs "
     [:a {:on-click (fn [_] (state/nav! :bot (:id (second (:bots data)))))} (:name (second (:bots data)))]]

    [:div.moves
     [match-results-view data]]]])
