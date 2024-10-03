(ns cyberleague.client.ui.user-card
  (:require
    [cyberleague.client.state :as state]))

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
    [:div.token-management
     [:h2 "CLI Token"
      [:a.button {:on-click state/new-cli-token!}
       "Reset"]]
     [:input {:disabled true
              :value    (str token)}]
     [:a.button {:on-click (fn [e]
                             (copy! {:element      (.-target e)
                                     :to-be-copied (str token)
                                     :message      "Copied"}))}
      "Copy"]]))

(defn user-card-view
  [{:card/keys [data] :as card}]
  (let [user data]
    [:div.card.user
     [:header
      [:span (str "@" (:user/name user))]
      [:div.gap]
      [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
     [:div.content
      (when (= (:user/id user)
               (:user/id @state/user))
        [token-management-view])
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
             (if (= :active (:bot/status bot)) "●" "○") " "
             (:bot/name bot)]]
           [:td (:bot/rating bot)]
           [:td
            [:a {:on-click (fn [_] (state/nav! :card.type/game (:game/id (:bot/game bot))))}
             (str "#" (:game/name (:bot/game bot)))]]])]]]]))
