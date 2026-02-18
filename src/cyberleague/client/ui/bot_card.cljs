(ns cyberleague.client.ui.bot-card
  (:require
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.common :as ui]
   [reagent.core :as r]
   [cljsjs.d3]))

(defn graph-view [history]
  [:div.graph
   {:ref (fn [el]
           (when el
             (js/window.bot_graph el (clj->js history))))}])

(defn bot-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let [data (state/tada-atom [:api/bot {:bot-id id}] {:refresh-rate 2500})]
    (when-let [bot @data]
      [:div.card.bot
       [:header
        [:span.bot-name [ui/bot-chip bot]]
        [:a.user-name {:on-click (fn [_] (state/nav! :card.type/user (:user/id (:bot/user bot))))}
         (str "@" (:user/name (:bot/user bot)))]
        [:a.game-name {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
         (str "#" (:game/name (:bot/game bot)))]
        [:div.gap]
        (when (= (:user/id @state/user) (:user/id (:bot/user bot)))
          [:a.button {:on-click (fn [_] (state/nav! :card.type/code (:bot/id bot)))} "CODE"])
        [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]

       [:div.content
        [:div.language (-> bot :bot/code :code/language)]
        [graph-view (:bot/history bot)]
        [:table.matches
         [:thead]
         [:tbody
          (for [match (->> (:bot/matches bot)
                         ;; ids are monotically increasing with time
                         ;; so can use them to order
                           (sort-by :match/id)
                           (reverse))]
            ^{:key (:match/id match)}
            [:tr
             [:td {:tw "text-right"}
              [:a {:on-click (fn [_] (state/nav! :card.type/match (:match/id match)))}
               (cond
                 (= (:match/winner match) (:bot/id bot))
                 "won"
                 (nil? (:match/winner match))
                 "tied"
                 (:match/error match)
                 "errored"
                 :else
                 "lost")]]
             [:td
              [:a {:on-click (fn [_] (state/nav! :card.type/match (:match/id match)))}
               " vs "
               (let [other-bot (->> (:match/bots match)
                                    (remove (fn [b] (= (:bot/id bot) (:bot/id b))))
                                    first)]
                 [ui/bot-chip other-bot])]]])]]]])))
