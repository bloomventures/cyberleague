(ns cyberleague.client.ui.games-card
  (:require
    [cyberleague.client.state :as state]))

(defn games-card-view
  [{:keys [data] :as card}]
  (let [games data]
    [:div.card.games
     [:header "GAMES"
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      [:table
       [:thead
        [:tr
         [:th "Name"]
         [:th "Active Bots"]]]
       [:tbody
        (for [game games]
          ^{:key (:id game)}
          [:tr
           [:td
            [:a {:on-click (fn [_] (state/nav! :game (:id game)))}
             (str "#" (:name game))]]
           [:td (:bot-count game)]])]]]]))
