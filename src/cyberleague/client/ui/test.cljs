(ns cyberleague.client.ui.test
  (:require
    [cyberleague.client.ui.match-results :refer [match-results-view]]))

(defn test-view
  [match bot]
  (when match
    [:div.test
     ;; match/info contains error information if there is an error
     (if (:match/info match)
       [:p (:match/info match)]
       [:<>
        [:p
         (cond
           (nil? (:winner match))
           "Tie!"

           (= (:bot/id bot) (:winner match))
           "You Won!"

           :else
           "You Lost!")]
        [match-results-view match]])]))
