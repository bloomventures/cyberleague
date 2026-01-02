(ns cyberleague.client.ui.user-card
  (:require
   [cyberleague.client.state :as state]
   [reagent.core :as r]
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
     {:tw "bg-#9fa8da -mb-1em -mx-1em p-1em text-white space-y-2"}
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
      [:div.card.user
       [:header
        [:span (str "@" (:user/name user))]
        [:div.gap]
        [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
       [:div.content
        {:tw "flex flex-col"}
        [:div.bots
         {:tw "grow"}
         [:table
          [:thead
           [:tr
            [:th "Bot"]
            [:th "Rating"]
            [:th "Game"]]]
          [:tbody
           (for [bot (:user/bots user)]
             ^{:key (:bot/id bot)}
             [:tr
              [:td
               [:a {:on-click (fn [_] (state/nav! :card.type/bot (:bot/id bot)))}
                [ui/bot-chip bot]]]
              [:td (:bot/rating bot)]
              [:td
               [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
                (str "#" (:game/name (:bot/game bot)))]]])]]]
        (when (= (:user/id user)
                 (:user/id @state/user))
          [token-management-view])]])))
