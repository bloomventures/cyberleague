(ns cyberleague.client.ui.user-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]))

(defn copy! [{:keys [element to-be-copied message]}]
  (let [original-text (.-innerText element)]
    (.. js/navigator
        -clipboard
        (writeText to-be-copied))
    (set! (.-innerText element) message)
    (js/setTimeout
     #(set! (.-innerText element) original-text) 6000)))

(defn token-management-view
  []
  (let [token (:user/cli-token @state/user)]
    [:section.token-management
     {:tw "bg-#9fa8da -mb-4 -mx-4 p-4 text-white space-y-2"}
     [:div {:tw "flex justify-between items-center"}
      [:h1 "CLI Token"]
      [ui/button {:on-click state/new-cli-token!}
       "Reset"]]
     [:div {:tw "flex"}
      [:input {:tw "p-1 grow border border-white bg-transparent border-r-0"
               :disabled true
               :value    (str token)}]
      [ui/button {:on-click (fn [e]
                              (copy! {:element      (.-target e)
                                      :to-be-copied (str token)
                                      :message      "Copied"}))}
       "Copy"]]]))

(defn user-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
    [data (state/tada-atom [:api/user {:other-user-id id}])]
    (when-let [user @data]
      [card/wrapper {}
       [card/header {:card card}
        [:<>
         [:span {:tw "mr-4"} (str "@" (:user/name user))]
         [:div {:tw "grow"}]]]
       [card/body {}
        [:<>
         [:div.bots
          {:tw "grow"}
          [:table
           {:tw "mx-auto"}
           [:thead
            [:tr
             [:th {:tw "text-left font-bold p-1"} "Bot"]
             [:th {:tw "text-left font-bold p-1"} "Rating"]
             [:th {:tw "text-left font-bold p-1"} "Game"]]]
           [:tbody
            (for [bot (:user/bots user)]
              ^{:key (:bot/id bot)}
              [:tr
               [:td {:tw "p-1"}
                [:a {:on-click (fn [_] (state/nav! :card.type/bot (:bot/id bot)))}
                 [ui/bot-chip bot]]]
               [:td {:tw "p-1"} (:bot/rating bot)]
               [:td {:tw "p-1"}
                [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
                 (str "#" (:game/name (:bot/game bot)))]]])]]]
         (when (= (:user/id user)
                  (:user/id @state/user))
           [token-management-view])]]])))
