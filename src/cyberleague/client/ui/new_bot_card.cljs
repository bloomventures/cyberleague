(ns cyberleague.client.ui.new-bot-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.state :as state]))

(defn new-bot-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
   [langs-and-envs (state/tada-atom [:api/languages])
    game (state/tada-atom [:api/game {:game-id id}])]
   [card/wrapper {}
    [card/header {:card card}
     "New bot (for " (:game/name @game) ")"]
    [card/body {}
     [:h2 {:tw "font-bold"} "Pick a language env:"]
     (for [language @langs-and-envs]
       ^{:key (:language/id language)}
       [:ul {:tw "list-disc px-3"}
        [:li (:language/slug language)
         [:ul {:tw "list-disc px-3"}
          (for [env (:language/envs language)]
            ^{:key (:env/id env)}
            [:li
             [:button {:tw "text-#3f51b5"
                       :on-click (fn [_]
                                   (-> (state/tada! [:api/create-bot!
                                                     {:game-slug (:game/slug @game)
                                                      :env-slug (:env/slug env)}])
                                       (.then (fn [bot]
                                                (state/close-card! card)
                                                (state/nav! :card.type/code (:bot/id bot))))))}
              (:env/slug env)]])]]])]]))
