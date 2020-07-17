(ns cyberleague.client.ui.app
  (:require
    [cyberleague.client.state :as state]
    [cyberleague.client.ui.styles :as styles]
    [cyberleague.client.ui.users-card :refer [users-card-view]]
    [cyberleague.client.ui.code-card :refer [code-card-view]]
    [cyberleague.client.ui.match-card :refer [match-card-view]]
    [cyberleague.client.ui.bot-card :refer [bot-card-view]]
    [cyberleague.client.ui.user-card :refer [user-card-view]]
    [cyberleague.client.ui.game-card :refer [game-card-view]]
    [cyberleague.client.ui.games-card :refer [games-card-view]]))

(defn app-view []
  [:div.app
   ;; LINKS ARE TEMPORARY

   [:link {:rel "stylesheet"
           :href "css/reset.css"}]
   [:link {:rel "stylesheet"
           :href "//fonts.googleapis.com/css?family=Inconsolata"}]
   [:link {:rel "stylesheet"
           :href "//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css"}]
   [:link {:rel "stylesheet"
           :href "//cdnjs.cloudflare.com/ajax/libs/codemirror/5.54.0/codemirror.min.css"}]

   [:style styles/css]

   ;; OK
   [:header
    [:h1 "Cyberleague"]
    [:h2 "Code bots to play games."]
    [:nav
     [:a {:on-click (fn [] (state/nav! :games nil))} "Games"]
     [:a {:on-click (fn [_] (state/nav! :users nil))} "Users"]
     (when-let [user @state/user]
       [:a.user {:on-click (fn [_] (state/nav! :user (:id user)))}
        [:img {:src (str "https://avatars.githubusercontent.com/u/" (user :gh-id) "?v=2&s=40")}]
        "My Bots"])
     (if-let [user @state/user]
       [:a.log-out {:on-click (fn [_] (state/log-out!))
                    :title "Log Out"}]
       [:a.log-in {:on-click (fn [_] (state/log-in!))}])]]
   [:div.cards
    (for [card @state/cards]
      (let [card-view (case (card :type)
                        :users users-card-view
                        :user user-card-view
                        :game game-card-view
                        :games games-card-view
                        :match match-card-view
                        :bot bot-card-view
                        :code code-card-view)]
        ^{:key (:url card)}
        [card-view card]))]])
