(ns cyberleague.game-registrar
  (:require
   [clojure.pprint :as pprint]
   [malli.core :as malli]
   [malli.error :as malli.error]
   [malli.util :as malli.util]
   [cyberleague.db.schema :as schema]))

(def games (atom {}))

(def MalliSchema
  some?)

(def BotBlueprint
  [:map
   [:blueprint/code string?]
   [:blueprint/env-slug :env/slug]])

(def schema
  [:map
   [:game.config/name string?]
   [:game.config/slug :game/slug] ;; ascii no spaces
   [:game.config/description string?]
   [:game.config/rules string?]
   [:game.config/match-results-view some?] ;; fn that takes a match, returns reagent data
   [:game.config/starter-code
    [:map-of
     :env/slug
     string?]]
   [:game.config/test-bot BotBlueprint]
   [:game.config/seed-bots
    [:vector BotBlueprint]]
   [:game.config/public-state-example some?]
   [:game.config/move-example some?]
   [:game.config/public-state-spec MalliSchema]
   [:game.config/internal-state-spec MalliSchema]
   [:game.config/move-spec MalliSchema]])

(defn register-game! [game]
  (if (malli/validate (malli.util/closed-schema schema) game)
    (swap! games assoc (:game.config/slug game) game)
    (let [cause (malli/explain (malli.util/closed-schema schema) game)]
      (throw
       (ex-info (str "Game config invalid\n"
                     (with-out-str
                       (pprint/pprint (malli.error/humanize cause)))) cause)))))
