(ns cyberleague.game-registrar
  (:require
   [clojure.pprint :as pprint]
   [malli.core :as malli]
   [malli.error :as malli.error]
   [malli.util :as malli.util]))

(defonce *games (atom {}))

(defn all []
  (vals @*games))

(defn by-slug [slug]
  (get @*games slug))

(def MalliSchema
  some?)

(def BotBlueprint
  [:map
   [:blueprint/code :string]
   [:blueprint/env-slug :string]])

(def schema
  [:map
   [:game.config/name :string]
   [:game.config/slug :string] ;; ascii no spaces
   [:game.config/description :string]
   [:game.config/rules :string]
   [:game.config/match-results-view :any] ;; fn that takes a match, returns reagent data
   [:game.config/test-bot BotBlueprint]
   [:game.config/seed-bots
    [:vector BotBlueprint]]
   [:game.config/technical-notes [:maybe :string]]
   [:game.config/context-spec MalliSchema]
   [:game.config/context-example :any]
   [:game.config/move-spec MalliSchema]
   [:game.config/move-example :any]
   [:game.config/state-spec MalliSchema]])

(defn register-game! [game]
  (if (malli/validate (malli.util/closed-schema schema) game)
    (swap! *games assoc (:game.config/slug game) game)
    (let [cause (malli/explain (malli.util/closed-schema schema) game)]
      (throw
       (ex-info (str "Game config invalid\n"
                     (with-out-str
                       (pprint/pprint (malli.error/humanize cause)))) cause)))))
