(ns cyberleague.client.ui.test
  (:require
    [cyberleague.client.ui.match-results :refer [match-results-view]]))

(defn test-view
  [match bot]
  [:div.test
   (when match
     [:p
      (cond
        (:error match)
        (:info match)

        (nil? (:winner match))
        "Tie!"

        (= (:bot/id bot) (:winner match))
        "You Won!"

        :else
        "You Lost!")])
   (when match
     [match-results-view match])])
