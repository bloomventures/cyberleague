(ns cyberleague.client.ui.match-results
  (:require
    [reagent.core :as r]
    [cyberleague.game-registrar :as registrar]))

(defn match-results-view [match]
  (r/with-let
    [view (get-in @registrar/games
            [(get-in match [:match/game :game/name])
             :game.config/match-results-view])
     state-history (match :match/state-history)
     max-value (dec (count state-history))
     move-index (r/atom max-value)]
    [:div.match-results
     [:div.scrubber
      [:div (str "Turn " @move-index "/" max-value)]
      [:input {:type "range"
               :min 0
               :max max-value
               :step 1
               :value @move-index
               :on-change (fn [e]
                            (reset! move-index (js/parseInt (.. e -target -value) 10)))}]]
     [view match
      (take (inc @move-index) state-history)
      (:history (get state-history @move-index))]]))
