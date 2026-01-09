(ns cyberleague.client.ui.test
  (:require
    [cyberleague.client.ui.match-results :refer [match-results-view]]
    [reagent.core :as r]))

(defn generic-results-view
  [match]
  (r/with-let
   [state-history (match :match/state-history)
    max-value (dec (count state-history))
    move-index (r/atom max-value)]
   [:div.match-results
    [:div.scrubber
     [:div.row
      [:button {:on-click (fn [] (swap! move-index dec))
                :disabled (= 0 @move-index)}
       "<"]
      (str "Turn " @move-index "/" max-value)
      [:button {:on-click (fn [] (swap! move-index inc))
                :disabled (= max-value @move-index)} ">"]]
     [:input {:type "range"
              :min 0
              :max max-value
              :step 1
              :value @move-index
              :on-change (fn [e]
                           (reset! move-index (js/parseInt (.. e -target -value) 10)))}]]

    [:details
     [:summary "state"]
     (pr-str (get state-history @move-index))]
    (pr-str (last (:history (get state-history @move-index))))]))

(defn test-view
  [match bot]
  [:div.test
   ;; match/info contains error information if there is an error
   (when match
     [:<>
      [:p
       (cond
         (:match/error match)
         "Bot Error"

         (nil? (:winner match))
         "Tie!"

         (= (:bot/id bot) (:winner match))
         "You Won!"

         :else
         "You Lost!")]
      [:div
       [generic-results-view match]
       [match-results-view match]]])])
