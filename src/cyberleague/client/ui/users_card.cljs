(ns cyberleague.client.ui.users-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]))

(defn users-card-view
  [card]
  (r/with-let
   [data (state/tada-atom [:api/users])]
   [card/wrapper {}
    [card/header
     {:card card}
     "USERS"]
    [card/body {}
     (when-let [users @data]
       [:table
        {:tw "mx-auto"}
        [:thead
         [:tr
          [:th {:tw "text-left font-bold p-1"} "Name"]
          [:th {:tw "text-left font-bold p-1"} "Active Bots"]]]
        [:tbody
         (for [user users]
           ^{:key (:user/id user)}
           [:tr
            [:td {:tw "p-1"}
             [:a {:on-click (fn [_] (state/nav! :card.type/user (:user/id user)))}
              (str "@" (:user/name user))]]
            [:td {:tw "p-1"} (:user/bot-count user)]])]])]]))
