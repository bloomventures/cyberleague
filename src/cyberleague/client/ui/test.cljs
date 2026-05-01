(ns cyberleague.client.ui.test
  (:require
   [cyberleague.client.ui.match-results :refer [match-results-view]]))

(defn test-view
  [match bot]
  [:div.test {:tw "h-full w-35% bg-gray-100"}
   (when match
     ^{:key (:match/id match)} ;; to force re-render
     [match-results-view
      {:message (cond
                  (contains? (:match/errors match) (:bot/id bot))
                  "Bot Errored"

                  (seq (disj (:match/errors match) (:bot/id bot)))
                  "Other Bot Errored"

                  (nil? (:winner match))
                  "Tie!"

                  (= (:bot/id bot) (:winner match))
                  "You Won!"

                  :else
                  "You Lost!")
       :match match}])])
