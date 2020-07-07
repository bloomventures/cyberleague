(ns cyberleague.client.ui.users-card
  (:require
    [cyberleague.client.state :as state]))

(defn users-card-view
  [{:keys [data] :as card}]
  (let [users data]
    [:div.card.users
     [:header "USERS"
      [:a.close
       {:on-click (fn [_]
                    (state/close-card! card))} "×"]]
     [:div.content
      [:table
       [:thead
        [:tr
         [:th "Name"]
         [:th "Active Bots"]]]
        [:tbody
         (for [user users]
           ^{:key (:id user)}
           [:tr
            [:td
             [:a {:on-click (fn [_] (state/nav! :user (:id user)))}
              (str "@" (:name user))]]
            [:td (:bot-count user)]])]]]]))
