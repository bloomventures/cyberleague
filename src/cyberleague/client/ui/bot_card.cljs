(ns cyberleague.client.ui.bot-card
  (:require
    [cyberleague.client.state :as state]
    [cljsjs.d3]))

(defn graph-view [history]
  [:div.graph
   {:ref (fn [el]
           (when el
             (js/window.bot_graph el (clj->js history))))}])

(defn get-record
  [bot]
  (let [match-results (map :match/winner (:bot/matches bot))
        bot-id (:bot/id bot)]
    {:wins (count (filter (partial = bot-id) match-results))
     :losses (count (filter (partial not= bot-id) match-results))
     :ties (count (filter (partial nil?) match-results))}))

(defn bot-card-view
  [{:card/keys [data] :as card}]
  (let [bot data
        record (get-record bot)]
    [:div.card.bot
     [:header
      [:span.bot-name (:bot/name bot)]
      [:a.user-name {:on-click (fn [_] (state/nav! :card.type/user (:user/id (:bot/user bot))))}
       (str "@" (:user/name (:bot/user bot)))]
      [:a.game-name {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
       (str "#" (:game/name (:bot/game bot)))]
      [:div.gap]
      (when (= (:user/id @state/user) (:user/id (:bot/user bot)))
        [:a.button {:on-click (fn [_] (state/nav! :card.type/code (:bot/id bot)))} "CODE"])
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      [:div.record
       [:p1 "W:" (:wins record) " L:" (:losses record) " T:" (:ties record)]]
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
           [:td
            [:a {:on-click (fn [_] (state/nav! :card.type/match (:match/id match)))}
             ;; case doesn't work here
             (cond
               (= (:match/winner match) (:bot/id bot))
               "won"
               (nil? (:match/winner match))
               "tied"
               :else
               "lost")
             " vs "
             (let [other-bot (->> (:match/bots match)
                                  (remove (fn [b] (= (:bot/id bot) (:bot/id b))))
                                  first)]
               (:bot/name other-bot))]]])]]]]))
