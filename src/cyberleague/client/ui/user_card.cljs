(ns cyberleague.client.ui.user-card
  (:require
    [cyberleague.client.state :as state]))

(defn user-card-view
  [{:keys [data] :as card}]
  (let [user data]
    [:div.card.user
     [:header
      [:span (str "@" (:name user))]
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      [:table
       [:thead
        [:tr
         [:th "Game"]
         [:th "Bot"]
         [:th "Rating"]]]
       [:tbody
        (for [bot (:bots user)]
          ^{:key (:id bot)}
          [:tr
           [:td
            [:a {:on-click (fn [_] (state/nav! :game (:id (:game bot))))}
             (str "#" (:name (:game bot)))]]
           [:td
            [:a {:on-click (fn [_] (state/nav! :bot (:id bot)))}
             (if (= :active (:status bot)) "●" "○") " "
             (:name bot)]]
           [:td (:rating bot)]])]]]]))
