(ns cyberleague.client.ui.bot-card
  (:require
    [cyberleague.client.state :as state]
    [cljsjs.d3]))

(defn graph-view [history]
  [:div.graph
   {:ref (fn [el]
           (when el
             (js/window.bot_graph el (clj->js history))))}])

(defn bot-card-view
  [{:keys [data] :as card}]
  (let [bot data]
    [:div.card.bot
     [:header
      [:span.bot-name (:name bot)]
      [:a.user-name {:on-click (fn [_] (state/nav! :user (:id (:user bot))))}
       (str "@" (:name (:user bot)))]
      [:a.game-name {:on-click (fn [_] (state/nav! :game (:id (:game bot))))}
       (str "#" (:name (:game bot)))]
      [:div.gap]
      (when (= (:id @state/user) (:id (:user bot)))
        [:a.button {:on-click (fn [_] (state/nav! :code (:id bot)))} "CODE"])
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]

     [:div.content
      [graph-view (:history data)]
      [:table.matches
       [:thead]
       [:tbody
        (for [match (->> (:matches bot)
                         ;; ids are monotically increasing with time
                         ;; so can use them to order
                         (sort-by :id)
                         (reverse))]
          ^{:key (:id match)}
          [:tr
           [:td
            [:a {:on-click (fn [_] (state/nav! :match (:id match)))}
             ;; case doesn't work here
             (cond
               (= (:winner match) (:id bot))
               "won"
               (nil? (:winner match))
               "tied"
               :else
               "lost")
             " vs "
             (let [other-bot (->> (:bots match)
                                  (remove (fn [b] (= (:id bot) (:id b))))
                                  first)]
               (:name other-bot))]]])]]]]))
