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
      [:span (:name bot)]
      [:a.user-name {:on-click (fn [_] (state/nav! :user (:id (:user bot))))}
       (str "@" (:name (:user bot)))]
      [:a.game-name {:on-click (fn [_] (state/nav! :game (:id (:game bot))))}
       (str "#" (:name (:game bot)))]
      (when (= (:id @state/user) (:id (:user bot)))
        [:a.button {:on-click (fn [_] (state/nav! :code (:id bot)))} "CODE"])
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]

     [:div.content
      [graph-view (:history data)]
      [:table.matches
       [:thead]
       [:tbody
        (for [match (:matches bot)]
          ^{:key (:id match)}
          [:tr
           [:td
            [:a {:on-click (fn [_] (state/nav! :match (:id match)))}
             (case (:winner match)
               nil "tied"
               (bot :id) "won"
               "lost")
             " vs "
             (let [other-bot (->> (:bots match)
                                  (remove (fn [b] (= (:id bot) (:id b))))
                                  first)]
               (:name other-bot))]]])]]]]))
