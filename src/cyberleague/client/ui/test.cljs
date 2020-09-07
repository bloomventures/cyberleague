(ns cyberleague.client.ui.test
  (:require
    [cyberleague.client.ui.match-results :refer [match-results-view]]))

(defn test-view
  [match bot]
  [:div.test
   (when match
     [:p
      (cond
        (:match/info match)
        (:match/info match)

        (nil? (:winner match))
        "Tie!"

        (= (:bot/id bot) (:winner match))
        "You Won!"

        :else
        "You Lost!")])
   (when (and match (nil? (:match/info match)))
     [match-results-view match])])
