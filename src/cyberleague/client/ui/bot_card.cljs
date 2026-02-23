(ns cyberleague.client.ui.bot-card
  (:require
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]
   [cljsjs.d3]
   [reagent.core :as r]))

(defn graph-view [history]
  [:div.graph
   {:ref (fn [el]
           (when el
             (js/window.bot_graph el (clj->js history))))}])

(defn record-summary
  [{:bot/keys [id matches]}]
  (->> matches
       (map :match/winner)
       (map (fn [winning-bot-id]
              (cond
                (= id winning-bot-id) :wins
                (nil? winning-bot-id) :ties
                :else :losses)))
       frequencies
       (merge {:wins 0 :losses 0 :ties 0})))

(defn bot-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let [data (state/tada-atom [:api/bot {:bot-id id}] {:refresh-rate 2500})]
    (when-let [bot @data]
      [card/wrapper {}
       [card/header {:card card}
        [:<>
         [:span {:tw "whitespace-nowrap"} [ui/bot-chip bot]]
         [ui/nav-link {:on-click (fn [_] (state/nav! :card.type/user (:user/id (:bot/user bot))))}
          (str "@" (:user/name (:bot/user bot)))]
         [ui/nav-link {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
          (str "#" (:game/name (:bot/game bot)))]
         [:div {:tw "grow"}]
         (when (= (:user/id @state/user) (:user/id (:bot/user bot)))
           [ui/nav-button {:on-click (fn [_] (state/nav! :card.type/code (:bot/id bot)))} "CODE"])]]
       [card/body {}
        [:<>
         [:div.language (-> bot :bot/code :code/language)]
         [graph-view (:bot/history bot)]
         (let [{:keys [wins losses ties]} (record-summary bot)]
           [:p {:tw "text-center"}
            "Wins: " wins ", Losses: " losses ", Ties: " ties])
         [:table.matches
          {:tw "self-center mt-4"}
          [:thead]
          [:tbody
           (for [match (->> (:bot/matches bot)
                            ;; ids are monotically increasing with time
                            ;; so can use them to order
                            (sort-by :match/id)
                            (reverse))]
             ^{:key (:match/id match)}
             [:tr
              [:td {:tw "text-right p-1"}
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
              [:td {:tw "p-1"}
               [:a {:on-click (fn [_] (state/nav! :card.type/match (:match/id match)))}
                " vs "
                (let [other-bot (->> (:match/bots match)
                                     (remove (fn [b] (= (:bot/id bot) (:bot/id b))))
                                     first)]
                  [ui/bot-chip other-bot])]]])]]]]])))

