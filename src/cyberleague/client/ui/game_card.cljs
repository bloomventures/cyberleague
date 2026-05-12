(ns cyberleague.client.ui.game-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]))

(defn game-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
   [data (state/tada-atom [:api/game {:game-id id}])]
   (when-let [game @data]
     [card/wrapper {}
      [card/header {:card card
                    :refresh [data]}
       [:<>
        [:span {:tw "whitespace-nowrap mr-4"}
         (str "#" (:game/slug game))]
        [:div {:tw "grow"}]]]
      [card/body {}
       [:div {:tw "max-w-45vw space-y-4"}
        [:div
         [ui/markdown (:game/description game)]]

        [ui/body-link {:on-click (fn []
                                   (state/nav! :card.type/game-standings id))}
         "View Standings"]

        [:div
         [ui/subheading "Rules"]
         [:div {:tw "py-2"}
          [ui/markdown (:game/rules game)]]]

        (when (:game/technical-notes game)
          [:div
           [ui/subheading "Technical Notes"]
           [:div {:tw "py-2"}
            [ui/markdown (:game/technical-notes game)]]])

        [:div
         [ui/subheading "Context (Bot Input) Example"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (js/JSON.stringify
            (js/JSON.parse
             (str (:game/context-example game)))
            nil
            2)]]]

        [:div
         [ui/subheading "Context Schema"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (ui/pretty-print
            (:game/context-spec game))]]]

        [:div
         [ui/subheading "Move (Bot Output) Example"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (str (:game/move-example game))]]]

        [:div
         [ui/subheading "Move Schema"]
         [:div {:tw "py-2"}
          [:code {:tw "whitespace-pre-wrap"}
           (ui/pretty-print (:game/move-spec game))]]]]]])))
