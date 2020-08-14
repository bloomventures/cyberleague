(ns cyberleague.client.ui.user-card
  (:require
    [cyberleague.client.state :as state]))

(defn user-card-view
  [{:card/keys [data] :as card}]
  (let [user data]
    [:div.card.user
     [:header
      [:span (str "@" (:user/name user))]
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      [:table
       [:thead
        [:tr
         [:th "Game"]
         [:th "Bot"]
         [:th "Rating"]]]
       [:tbody
        (for [bot (:user/bots user)]
          ^{:key (:bot/id bot)}
          [:tr
           [:td
            [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
             (str "#" (:game/name (:bot/game bot)))]]
           [:td
            [:a {:on-click (fn [_] (state/nav! :card.type/bot (:bot/id bot)))}
             (if (= :active (:bot/status bot)) "●" "○") " "
             (:bot/name bot)]]
           [:td (:bot/rating bot)]])]]]]))
