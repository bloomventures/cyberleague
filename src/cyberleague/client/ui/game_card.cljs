(ns cyberleague.client.ui.game-card
  (:require
   [markdown.core :as markdown]
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]))

(defn game-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
    [data (state/tada-atom [:api/game {:game-id id}] {:refresh-rate 5000})]
    (when-let [game @data]
      [card/wrapper {}
       [card/header {:card card}
        [:<>
         [:span {:tw "whitespace-nowrap mr-4"}
          (str "#" (:game/name game))]
         [:div {:tw "grow"}]
         (when @state/user
           [ui/nav-button {:on-click (fn [_]
                                       (-> (state/tada! [:api/create-bot! {:game-id (:game/id game)}])
                                           (.then (fn [data]
                                                    (state/nav! :card.type/code (:id data))))))}
            "CREATE A BOT"])]]
       [card/body {}
        [:<>
         [:div {:tw "max-w-30em"
                :dangerouslySetInnerHTML
                (r/unsafe-html (markdown/md->html (:game/description game)))}]
         [:table
          {:tw "mx-auto"}
          [:thead
           [:tr
            [:th {:tw "text-left font-bold p-1"}]
            [:th {:tw "text-left font-bold p-1"} "Rank"]
            [:th {:tw "text-left font-bold p-1"} "Bot"]
            [:th {:tw "text-left font-bold p-1"} "Rating"]
            [:th {:tw "text-left font-bold p-1"}]]]
          [:tbody
           (let [max-bar-width 100
                 max-rating (apply max (map :bot/rating (:game/bots game)))
                 ->width (fn [rating]
                           (* max-bar-width (/ rating max-rating)))]
             (for [[rank bot] (->> (:game/bots game)
                                   (sort-by :bot/rating)
                                   reverse
                                   (map-indexed vector))]
               ^{:key (:bot/id bot)}
               [:tr
                [:td {:tw "p-1"} (when (= (:db/id (:bot/user bot)) (:user/id @state/user))
                                   "★")]
                [:td {:tw "p-1"} (inc rank)]
                [:td {:tw "p-1"}
                 [:a {:on-click (fn [_]
                                  (state/nav! :card.type/bot (:bot/id bot)))}
                  [ui/bot-chip bot]]]
                [:td {:tw "p-1"} (:bot/rating bot)]
                [:td {:tw "p-1 align-middle"}
                 [:div {:tw "bg-#6877ca"
                        :style {:width (str (->width (:bot/rating bot)) "px")
                                :height "0.5em"}}]]]))]]]]])))
