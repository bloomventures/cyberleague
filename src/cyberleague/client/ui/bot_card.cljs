(ns cyberleague.client.ui.bot-card
  (:require
   [cljsjs.d3]
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]))

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

(defn result-status
  [{:keys [match bot-id]}]
  (cond
    (contains? (:match/errors match) bot-id)
    "errored"

    (= (:bot/id (:match/winner match)) bot-id)
    "won"

    (nil? (:match/winner match))
    "tied"

    (:match/winner match)
    "lost"

    :else
    "other"))

(defn bot-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
   [data (state/tada-atom [:api/bot {:bot-id id}] {:refresh-rate 2500})]
   [card/wrapper {}
    [card/header {:card card
                  :refresh [data]}
     (when-let [bot @data]
       [:<>
        [:span {:tw "whitespace-nowrap"} [ui/bot-chip bot]]
        [ui/nav-link {:on-click (fn [_] (state/nav! :card.type/user (:user/id (:bot/user bot))))}
         (str "@" (:user/name (:bot/user bot)))]
        [ui/nav-link {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
         (str "#" (:game/name (:bot/game bot)))]
        [:div {:tw "grow"}]])]
    [card/body {}
     (when-let [bot @data]
       [:<>
        [:div.env (str (-> bot :bot/active-artifact :artifact/env :env/language :language/slug))]
        [graph-view (:bot/history bot)]
        (let [{:keys [wins losses ties]} (record-summary bot)]
          [:p {:tw "text-center"}
           "Wins: " wins ", Losses: " losses ", Ties: " ties])
        [:table.matches
         {:tw "self-center mt-4"}
         [:thead]
         [:tbody
          (for [match (->> (:bot/matches bot)
                           (sort-by :match/timestamp >))]
            ^{:key (:match/id match)}
            [:tr
             [:td {:tw "px-1"}
              (when (:match/test? match)
                [:div {:tw "text-xs bg-#3f51b5  text-white px-1 rounded"} "TEST"])]
             [:td {:tw "px-1 text-gray-400"} (.toLocaleString (:match/timestamp match))]
             [:td {:tw "text-right px-1"}
              [:a {:on-click (fn [_] (state/nav! :card.type/match (:match/id match)))}
               (result-status {:match match
                               :bot-id (:bot/id bot)})]]
             [:td {:tw "p-1"}
              [:a {:on-click (fn [_] (state/nav! :card.type/match (:match/id match)))}
               " vs "
               (let [other-bot (->> (:match/bots match)
                                    (remove (fn [b] (= (:bot/id bot) (:bot/id b))))
                                    first)]
                 [ui/bot-chip other-bot])]]])]]])]]))

