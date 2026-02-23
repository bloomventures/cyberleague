(ns cyberleague.client.ui.games-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]))

(defn games-card-view
  [card]
  (r/with-let
    [data (state/tada-atom [:api/games])]
    (when-let [games @data]
      [card/wrapper {}
       [card/header {:card card} "GAMES"]
       [card/body {}
        [:table
         {:tw "mx-auto"}
         [:thead
          [:tr
           [:th {:tw "text-left font-bold p-1"} "Name"]
           [:th {:tw "text-left font-bold p-1"} "Active Bots"]]]
         [:tbody
          (for [game games]
            ^{:key (:game/id game)}
            [:tr
             [:td {:tw "p-1"}
              [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id game)))}
               (str "#" (:game/name game))]]
             [:td {:tw "p-1"} (:game/bot-count game)]])]]]])))
