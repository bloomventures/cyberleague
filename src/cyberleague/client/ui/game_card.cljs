(ns cyberleague.client.ui.game-card
  (:require
    [cyberleague.client.state :as state]
    [markdown.core :as markdown]))

(defn game-card-view
  [{:keys [data] :as card}]
  (let [game data]
    [:div.card.game
     [:header
      [:span.game-name
       (str "#" (:name game))]
      [:div.gap]
      (when @state/user
        [:a.button {:on-click (fn [_] (state/new-bot! (:id game)))} "CREATE A BOT"])
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      [:div {:dangerouslySetInnerHTML #js {:__html (markdown/md->html (:description game))}}]
      [:table
       [:thead
        [:tr
         [:th "Rank"]
         [:th "Bot"]
         [:th "Rating"]]]
       [:tbody
        (->> (:bots game)
             (sort-by :rating)
             reverse
             (map-indexed (fn [rank bot]
                            ^{:key (:id bot)}
                            [:tr
                             [:td rank]
                             [:td
                              [:a {:on-click (fn [_]
                                               (state/nav! :bot (:id bot)))}
                               (if (= :active (:status bot))
                                 "●"
                                 "○")
                               " "
                               (:name bot)]]
                             [:td (:rating bot)]])))]]]]))
