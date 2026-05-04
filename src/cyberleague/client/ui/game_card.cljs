(ns cyberleague.client.ui.game-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]))

(defn subheading [s]
  [:div {:tw "border-b border-solid text-#3f51b5 border-#3f51b5"}
   s])

(defn game-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
   [data (state/tada-atom [:api/game {:game-id id}] {:refresh-rate 5000})]
   (when-let [game @data]
     [card/wrapper {}
      [card/header {:card card
                    :refresh [data]}
       [:<>
        [:span {:tw "whitespace-nowrap mr-4"}
         (str "#" (:game/name game))]
        [:div {:tw "grow"}]]]
      [card/body {}
       [:div {:tw "max-w-45vw space-y-4"}
        [:div
         [ui/markdown (:game/description game)]]
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
               [:td {:tw "p-1"} (when (= (:user/id (:bot/user bot)) (:user/id @state/user))
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
                               :height "0.5em"}}]]]))]]
        [:div
         [subheading "Rules"]
         [:div {:tw "py-2"}
          [ui/markdown (:game/rules game)]]]

        [:div
         [subheading "Technical Notes"]
         [:div {:tw "py-2"}
          [ui/markdown (:game/technical-notes game)]]]

        [:div
         [subheading "Context (Bot Input) Example"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (js/JSON.stringify
            (js/JSON.parse
             (str (:game/context-example game)))
            nil
            2)]]]

        [:div
         [subheading "Context Schema"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (ui/pretty-print
            (str (:game/context-spec game)))]]]

        [:div
         [subheading "Move (Bot Output) Example"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (str (:game/move-example game))]]]

        [:div
         [subheading "Move Schema"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (ui/pretty-print (str (:game/move-spec game)))]]]
        ]]])))
