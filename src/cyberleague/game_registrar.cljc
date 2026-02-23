(ns cyberleague.game-registrar
  (:require
   [clojure.pprint :as pprint]
   [malli.core :as malli]
   [malli.error :as malli.error]
   [malli.util :as malli.util]))

(def games (atom {}))

(def MalliSchema
  some?)

(def schema
  [:map
   [:game.config/name string?]
   [:game.config/description string?]
   [:game.config/rules string?]
   [:game.config/match-results-view some?] ;; fn that takes a match, returns reagent data
   [:game.config/starter-code
    [:map-of string?  string?]]
   [:game.config/test-bot string?]
   [:game.config/seed-bots
    [:vector
     [:map
      [:code/code string?]
      [:code/language string?]]]]
   [:game.config/public-state-example some?]
   [:game.config/move-example some?]
   [:game.config/public-state-spec MalliSchema]
   [:game.config/internal-state-spec MalliSchema]
   [:game.config/move-spec MalliSchema]])

(defn register-game! [game]
  (if (malli/validate (malli.util/closed-schema schema) game)
    (swap! games assoc (game :game.config/name) game)
    (let [cause (malli/explain (malli.util/closed-schema schema) game)]
      (throw
       (ex-info (str "Game config invalid\n"
                     (with-out-str
                       (pprint/pprint (malli.error/humanize cause)))) cause)))))
