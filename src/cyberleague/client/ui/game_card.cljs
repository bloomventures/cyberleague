(ns cyberleague.client.ui.game-card
  (:require
    [cyberleague.client.state :as state]
    [markdown.core :as markdown]))

(defn game-card-view
  [{:card/keys [data] :as card}]
  (let [game data]
    [:div.card.game
     [:header
      [:span.game-name
       (str "#" (:game/name game))]
      [:div.gap]
      (when @state/user
        [:a.button
         {:on-click (fn [_]
                      (-> (state/tada! [:api/create-bot! {:game-id (:game/id game)}])
                          (.then (fn [data]
                                   (state/nav! :card.type/code (:id data))))))}
         "CREATE A BOT"])
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      [:div {:dangerouslySetInnerHTML #js {:__html (markdown/md->html (:game/description game))}}]
      [:table
       [:thead
        [:tr
         [:th]
         [:th "Rank"]
         [:th "Bot"]
         [:th "Rating"]
         [:th]]]
       [:tbody
        (let [max-bar-width 100
              max-rating (apply max (map :bot/rating (:game/bots game)))
              ->width (fn [rating]
                        (* max-bar-width (/ rating max-rating)))]
          (->> (:game/bots game)
               (sort-by :bot/rating)
               reverse
               (map-indexed (fn [rank bot]
                              ^{:key (:bot/id bot)}
                              [:tr
                               [:td (when (= (:bot/user-id bot) (:user/id @state/user))
                                      "★")]
                               [:td (inc rank)]
                               [:td
                                [:a {:on-click (fn [_]
                                                 (state/nav! :card.type/bot (:bot/id bot)))}
                                 (if (= :active (:bot/status bot))
                                   "●"
                                   "○")
                                 " "
                                 (:bot/name bot)]]
                               [:td (:bot/rating bot)]
                               [:td
                                [:div.bar {:style {:width (->width (:bot/rating bot))}}]]]))))]]]]))
