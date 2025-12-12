(ns cyberleague.client.ui.users-card
  (:require
   [cyberleague.client.state :as state]
   [reagent.core :as r]))

(defn users-card-view
  [card]
  (r/with-let
    [data (state/tada-atom [:api/users])]
    (when-let [users @data]
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
            ^{:key (:user/id user)}
            [:tr
             [:td
              [:a {:on-click (fn [_] (state/nav! :card.type/user (:user/id user)))}
               (str "@" (:user/name user))]]
             [:td (:user/bot-count user)]])]]]])))
