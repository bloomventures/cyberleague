(ns cyberleague.client.ui.match-card
  (:require
    [cyberleague.client.state :as state]
    [cyberleague.client.ui.match-results :refer [match-results-view]]))

(defn match-card-view
  [{:card/keys [data] :as card}]
  (let [match data]
    [:div.card.match
     [:header "MATCH"
      [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:match/game match))))} (str "#" (:game/name (:match/game match)))]
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      (let [[bot1 bot2] (:match/bots match)]
        [:h1
         [:a {:on-click (fn [_] (state/nav! :card.type/bot (:bot/id bot1)))} (:bot/name bot1)]
         " vs "
         [:a {:on-click (fn [_] (state/nav! :card.type/bot (:bot/id bot2)))} (:bot/name bot2)]])

      [match-results-view match]]]))
