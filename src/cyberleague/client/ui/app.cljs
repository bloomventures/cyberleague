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
   {:tw "h-screen flex flex-col bg-#eeeeee"
    :style {:font-family "Inconsolata"
            :font-size "14px"}}

   ;; LINKS ARE TEMPORARY
   [:link {:rel "stylesheet"
           :href "/reset.css"}]
   [:link {:rel "stylesheet"
           :href "//fonts.googleapis.com/css?family=Inconsolata"}]
   [:link {:rel "stylesheet"
           :href "//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css"}]
   [:link {:rel "stylesheet"
           :href "//cdnjs.cloudflare.com/ajax/libs/codemirror/5.54.0/codemirror.min.css"}]

   [:style styles/css]

   ;; OK
   [:header
    {:tw "p-5 flex justify-between items-center gap-4"}
    [:h1
     {:tw "font-bold leading-2em"}
     "Cyberleague"]
    [:h2
     {:tw "leading-2em grow"}
     "Code bots to play games."]
    [:nav
     {:tw "flex items-center gap-4"}
     [:a {:on-click (fn [] (state/nav! :card.type/games nil))} "Games"]
     [:a {:on-click (fn [_] (state/nav! :card.type/users nil))} "Users"]
     (if-let [user @state/user]
       [:<>
        [:a {:on-click (fn [_] (state/nav! :card.type/user (:user/id user)))} "My Bots"]
        [:a {:tw "inline-flex text-white/65 hover:text-white"
             :on-click (fn [_] (state/nav! :card.type/user (:user/id user)))}
         [:img {:tw "bg-#9fa8da h-2em w-2em rounded-l"
                :src (str "https://avatars.githubusercontent.com/u/" (user :user/github-id) "?v=2&s=40")}]
         [:span {:tw "bg-#3f51b5 rounded-r flex items-center px-2"}
          (user :user/name)]]
        [:a {:tw "-mx-3"
             :on-click (fn [_] (state/log-out!))
             :title "Log Out"}
         "×"]]
       [:a {:tw "px-3 py-2 rounded bg-#3f51b5 text-white/65 hover:text-white"
            :on-click (fn [_] (state/log-in!))} "Log In"])]]

   [:div.cards
    {:tw "pb-5 grow flex overflow-x-auto gap-4 px-4"}
    (for [[type params :as card] @state/cards]
      (let [card-view (case type
                        :card.type/users users-card-view
                        :card.type/user user-card-view
                        :card.type/game game-card-view
                        :card.type/games games-card-view
                        :card.type/match match-card-view
                        :card.type/bot bot-card-view
                        :card.type/code code-card-view)]
        ^{:key card}
        [card-view card]))]])
