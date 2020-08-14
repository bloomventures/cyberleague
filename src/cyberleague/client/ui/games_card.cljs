(ns cyberleague.client.ui.games-card
  (:require
    [cyberleague.client.state :as state]))

(defn games-card-view
  [{:card/keys [data] :as card}]
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
          ^{:key (:game/id game)}
          [:tr
           [:td
            [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id game)))}
             (str "#" (:game/name game))]]
           [:td (:game/bot-count game)]])]]]]))
