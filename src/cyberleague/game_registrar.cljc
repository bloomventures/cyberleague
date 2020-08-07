(ns cyberleague.game-registrar
  (:require
   [clojure.pprint :as pprint]
   [malli.core :as malli]
   [malli.util :as malli.util]
   [malli.error :as malli.error]))

(def games (atom {}))

(def schema
  [:map
   [:game.config/name string?]
   [:game.config/description string?]
   [:game.config/rules string?]
   [:game.config/match-results-view some?] ;; fn that takes a match, returns reagent data
   [:game.config/match-results-styles some?] ;; fn that takes no args, returns garden data
   [:game.config/starter-code
    [:map-of string?  string?]]
   [:game.config/test-bot string?]
   [:game.config/seed-bots
    [:vector string?]]])

(defn register-game! [game]
  (if (malli/validate (malli.util/closed-schema schema) game)
    (swap! games assoc (game :game.config/name) game)
    (let [cause (malli/explain (malli.util/closed-schema schema) game)]
      (throw
       (ex-info (str "Game config invalid\n"
                     (with-out-str
                       (pprint/pprint (malli.error/humanize cause)))) cause)))))
