(ns cyberleague.games.goofspiel.core
  (:require
   [cyberleague.common.schema :as s]
   [cyberleague.game-registrar]
   [cyberleague.games.goofspiel.bots :as bots]
   [cyberleague.games.goofspiel.engine] ;; so it gets registered
   [cyberleague.games.goofspiel.ui :as ui]))

(def Card
  [:int {:min 1 :max 13}])

#_(malli.core/validate Card 13)

(cyberleague.game-registrar/register-game!
 {:game.config/name
  "goofspiel"

  :game.config/slug
  "goofspiel"

  :game.config/description
  "Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a 2-player card game with simple rules but surprising depth."

  :game.config/rules
  (str
   "- Take three suits from a standard deck. One suit becomes the trophy deck (shuffled face-down); each player takes one of the remaining suits as their hand.\n"
   "- Each round, flip the top trophy card face-up. Both players simultaneously choose a card from their hand and reveal it.\n"
   "- The player who played the higher card wins the trophy card (scoring its face value: Ace=1, Jack=11, Queen=12, King=13). If there's a tie, the trophy card is discarded and neither player scores.\n"
   "- The played cards are removed from both hands, and the next round begins.\n"
   "- After all 13 rounds, the player with the highest total score wins.\n")

  :game.config/technical-notes
  "Cards are represented as integers 1–13 (Ace=1, Jack=11, Queen=12, King=13). The trophy deck is shuffled randomly at the start of each match."

  :game.config/state-spec
  [:map
   [:player-cards
    [:map-of s/PlayerId [:set Card]]]
   [:trophy-cards [:set Card]]
   [:current-trophy [:maybe Card]]
   [:history
    [:vector
     [:map-of
      [:or s/PlayerId [:enum :trophy]]
      Card]]]]

  :game.config/context-spec
  [:map
   [:player-cards
    [:map
     [:me [:set Card]]
     [:opponent [:set Card]]]]
   [:trophy-cards [:set Card]]
   [:current-trophy [:maybe Card]]
   [:history
    [:vector
     [:map
      [:me Card]
      [:opponent Card]
      [:trophy Card]]]]]

  :game.config/context-example
  {:player-cards
   {:opponent #{1 3 4 5 7 8 9 10 11 12 13}
    :me #{1 2 3 5 6 7 9 10 11 12 13}}
   :trophy-cards #{1 3 4 5 7 8 9 10 12 13}
   :current-trophy 11
   :history [{:me 8 :opponent 6 :trophy 6}
             {:me 4 :opponent 2 :trophy 2}]}

  :game.config/move-spec
  Card

  :game.config/move-example
  5

  :game.config/match-results-view
  ui/match-results-view

  :game.config/test-bot
  {:blueprint/env-slug "clojure-sci"
   :blueprint/code (pr-str bots/random-bot)}

  :game.config/seed-bots
  [{:blueprint/env-slug "clojure-sci"
    :blueprint/code (pr-str bots/random-bot)}
   {:blueprint/env-slug "clojure-sci"
    :blueprint/code (pr-str bots/current-trophy-bot)}
   {:blueprint/env-slug "clojure-sci"
    :blueprint/code (pr-str bots/other-bot)}]})
