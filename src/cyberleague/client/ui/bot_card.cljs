(ns cyberleague.client.ui.bot-card
  (:require
   [clojure.string :as string]
   [cljsjs.d3]
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]))

(defn graph-view [history matches bot-id]
  (let [matches-by-id (zipmap (map :match/id matches)
                              matches)]
    [:div.graph
     {:ref (fn [el]
             (when el
               (js/window.bot_graph el
                                    (->> history
                                         (map (fn [e]
                                                (let [digest (->> (:match-id e)
                                                                  matches-by-id
                                                                  :match/artifacts
                                                                  (filter (fn [a]
                                                                            (= bot-id (:bot/id (:artifact/bot a)))))
                                                                  first
                                                                  :artifact/digest)]
                                                  (assoc e
                                                         :digest digest
                                                         :color (ui/color digest)))))
                                         clj->js))))}]))

(defn record-summary
  [{:bot/keys [id matches]}]
  (->> matches
       (remove :match/test?)
       (map :match/winning-bots)
       (map (fn [winning-bots]
              (cond
                (contains? (set winning-bots) id) :wins
                (empty? winning-bots) :ties
                :else :losses)))
       frequencies
       (merge {:wins 0 :losses 0 :ties 0})))

(defn result-status
  [{:keys [match bot-id]}]
  (cond
    (contains? (set (map :bot/id (:match/disqualified-bots match))) bot-id)
    "errored"

    (and (= 1 (count (:match/winning-bots match)))
         (contains? (set (map :bot/id (:match/winning-bots match))) bot-id))
    "won"

    (contains? (set (map :bot/id (:match/winning-bots match))) bot-id)
    "tied"

    (seq (:match/winning-bots match))
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
        [:div {:tw "grow"}]])]
    [card/body {}
     (when-let [bot @data]
       [:<>
        [:table
         [:tbody
          [:tr
           [:td "User"]
           [:td [:a {:on-click (fn [_] (state/nav! :card.type/user (:user/id (:bot/user bot))))}
                 (str "@" (:user/name (:bot/user bot)))]]]
          [:tr
           [:td "Game"]
           [:td [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
                 (str "#" (:game/name (:bot/game bot)))]]]
          [:tr
           [:td "Active Artifact"]
           [:td (if (:bot/active-artifact bot)
                  [ui/artifact-chip (-> bot :bot/active-artifact :artifact/digest)]
                  "(none)")]]
          [:tr
           [:td "Language"]
           [:td [:div.env (str (-> bot :bot/active-artifact :artifact/env :env/language :language/slug))]]]
          [:tr
           [:td "Env"]
           [:td [:div.env (str (-> bot :bot/active-artifact :artifact/env :env/slug))]]]

          [:tr
           [:td "Weight"]
           [:td (-> bot :bot/active-artifact :artifact/weight)]]]]

        [graph-view (:bot/history bot) (:bot/matches bot) (:bot/id bot)]

        (let [{:keys [wins losses ties]} (record-summary bot)]
          [:p {:tw "text-center"}
           "Wins: " wins ", Losses: " losses ", Ties: " ties])


        [:div {:tw "mt-4"}
         [ui/subheading "Matches"]
         [:div {:tw "py-2"}
          [:table
           {:tw "self-center"}
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
               [:td (let [digest (->> match
                                      :match/artifacts
                                      (filter (fn [a]
                                                (= (:bot/id bot)
                                                   (:bot/id (:artifact/bot a)))))
                                      first
                                      :artifact/digest)]
                      [ui/artifact-chip digest]) ]
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
                   [ui/bot-chip other-bot])]]])]]]]

        [:div {:tw "mt-8"}
         [ui/subheading "Artifacts"]
         [:div {:tw "py-2"}
          (for [artifact (:bot/artifacts bot)]
            [:table
             [:tbody
              [:tr
               [:td {:tw "px-1 text-gray-400"}
                (.toLocaleString (:artifact/created-at artifact))]
               [:td
                [ui/artifact-chip (:artifact/digest artifact)]]]]])]]])]]))

