(ns cyberleague.client.ui.match-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]
   [cyberleague.client.ui.match-results :refer [match-results-view]]))

(defn match-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
   [data (state/tada-atom [:api/match {:match-id id}])]
   (when-let [match @data]
     [card/wrapper {}
      [card/header {:card card
                    :refresh [data]}
       [:<>
        "MATCH"
        [ui/nav-link {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:match/game match))))}
         (str "#" (:game/name (:match/game match)))]]]
      [card/body {}
       [:<>
        [:div {:tw "flex justify-center mb-2"}
         (when (:match/test? match)
           [:div {:tw "bg-#3f51b5  text-white px-1 rounded"} "TEST"])]


        [:table
         [:tbody
          [:tr
           [:td "Match Id"]
           [:td (str (:match/id match))]]
          [:tr
           [:td "Time"]
           [:td (.toLocaleString (:match/timestamp match))]]
          [:tr
           [:td "Players"]
           [:td
            (for [bot (:match/bots match)]
              [:div [:a {:on-click (fn [_] (state/nav! :card.type/bot (:bot/id bot)))} [ui/bot-chip bot]] " as " (get-in match [:match/player-mappings (:bot/id bot)])])]]
          [:tr
           [:td "Winner(s):"]
           [:td (for [{:bot/keys [id]} (:match/winning-bots match)]
                  [ui/bot-chip (->> (:match/bots match)
                                    (filter (fn [b]
                                              (= id (:bot/id b))))
                                    first)])]]
          [:tr
           [:td "Disqualified:"]
           [:td
            (for [{:bot/keys [id]} (:match/disqualified-bots match)]
              [ui/bot-chip (->> (:match/bots match)
                                (filter (fn [b]
                                          (= id (:bot/id b))))
                                first)])]]]]
        [match-results-view {:match match}]]]])))
