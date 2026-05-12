(ns cyberleague.games.liars-dice.core
  (:require
   [cyberleague.common.schema :as s]
   [cyberleague.game-registrar]
   [cyberleague.games.liars-dice.bots :as bots]
   [cyberleague.games.liars-dice.engine] ;; so it gets registered
   #?@(:cljs [[cyberleague.games.liars-dice.ui :as ui]])))

(def Die
  [:int {:min 1 :max 6}])

(def Move
  [:or
   [:map
    [:action [:= "bid"]]
    [:quantity [:int {:min 1 :max 10}]]
    [:face Die]]
   [:map
    [:action [:= "challenge"]]]])

(def HistoryEntry
  [:map
   [:player-id s/PlayerId]
   [:move Move]])

(cyberleague.game-registrar/register-game!
 {:game.config/name
  "Liar's Dice"

  :game.config/slug
  "liars-dice"

  :game.config/description
  (str "A classic bluffing game where players bid on what dice are showing across all hidden hands, then call each other's bluff.\n"
       "This challenge implements a single round of the original game.")

  :game.config/rules
  (str
   "- Each player rolls 5 dice in secret.\n"
   "- Players take turns bidding by announcing a face value and a minimum count (e.g. \"three 4s\" means at least three 4s exist across both players' dice).\n"
   "- Each new bid must be strictly higher than the last: either a higher quantity (any face), or the same quantity with a higher face.\n"
   "- On your turn you may instead challenge the previous bid.\n"
   "- When challenged, all dice are revealed. If the actual count meets or exceeds the bid, the bidder wins. Otherwise the challenger wins.\n"
   "- 1s are \"wild\": they always count toward the current bid face.\n")

  :game.config/technical-notes
  nil

  :game.config/state-spec
  [:map
   [:dice [:map-of s/PlayerId [:vector Die]]]
   [:history [:vector HistoryEntry]]]

  :game.config/context-spec
  [:map
   [:my-id s/PlayerId]
   [:my-dice
    [:vector
     {:min 5 :max 5}
     Die]]
   [:history
    [:vector HistoryEntry]]]

  :game.config/context-example
  {:my-id 1
   :my-dice [3 1 2 2 5]
   :history [{:player-id 0
              :move {:action "bid" :quantity 2 :face 3}}
             {:player-id 1
              :move {:action "bid" :quantity 2 :face 5}}
             {:player-id 0
              :move {:action "bid" :quantity 3 :face 5}}]}

  :game.config/move-spec
  Move

  :game.config/move-example
  {:action "bid" :quantity 3 :face 4}

  :game.config/match-results-view
  #?(:cljs ui/match-results-view
     :clj nil)

  :game.config/test-bot
  {:blueprint/env-slug "clojure-sci"
   :blueprint/code (pr-str bots/random-bot)}

  :game.config/seed-bots
  [{:blueprint/env-slug "clojure-sci"
    :blueprint/code (pr-str bots/random-bot)}
   {:blueprint/env-slug "clojure-sci"
    :blueprint/code (pr-str bots/counting-bot)}]})
