(ns cyberleague.games.goofspiel.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.goofspiel.bots :as bots]
   [cyberleague.games.goofspiel.engine] ;; so it gets registered
   [cyberleague.games.goofspiel.starter-code :as starter-code]
   [cyberleague.games.goofspiel.ui :as ui]))

(def UserId
  integer?)

(def Card
  [:and
   integer?
   [:>= 1]
   [:<= 13]])

(cyberleague.game-registrar/register-game!
 {:game.config/name "goofspiel"
  :game.config/description
  (str "Also known as the Game of Perfect Strategy (GoPS), Goofspiel is a card game with simple rules but surprising depth.\n"
       "3 suits are taken from the game, one is yours to play with, another is your opponent's, and the third is the trophy deck (what you fight over). Each round, one of the trophy cards is revealed, then you and your opponent simultaneously bid one of your cards. The player with the higher bid scores the value of the trophy card. If there's a tie, no one scores. The spent trophy and bid cards are removed, and the next round is played. Once all cards have been played, the scores are tallied: the winner is the player with the most points (the sum of the value of the trophy cards they scored).")
  :game.config/rules
  (str "For our purposes, the game is played with the integers 1 through 12.\n"
       "## Function Input:\n"
       "\n"
       "{:your-cards #{ 1 2 3 13 }
        :their-cards #{ 1 2 3 13 }
        :trophy-cards #{ 1 2 3 13 }
        :current-trophy 4
        :history [ { :you 1 :them 1 :trophy 1 } … ] } }\n"
       "\n"
       "## Expected Output:\n"
       "\n"
       "a        ; a is the integer corresponding to your bid, it must be an integer that is still in your deck")
  :game.config/move-example  5
  :game.config/public-state-example {:your-cards #{1 2 3 6 7 8 9 10 11 12 13}
                                     :their-cards #{1 2 4 5 6 8 9 10 11 12 13}
                                     :trophy-cards #{2 3 4 5 6 7 9 11 12 13}
                                     :current-trophy 1
                                     :history [{:you 4 :them 3 :trophy 8}
                                               {:you 5 :them 7 :trophy 10}]}
  :game.config/internal-state-spec [:map
                                    [:player-cards
                                     [:map-of UserId [:set Card]]]
                                    [:trophy-cards [:set Card]]
                                    [:current-trophy Card]
                                    [:history
                                     [:vector
                                      [:map-of
                                       [:or UserId [:enum :trophy]]
                                       Card]]]]
  :game.config/public-state-spec [:map
                                  [:your-cards [:set Card]]
                                  [:their-cards [:set Card]]
                                  [:trophy-cards [:set Card]]
                                  [:current-trophy Card]
                                  [:history
                                   [:vector
                                    [:map
                                     [:you Card]
                                     [:them Card]
                                     [:trophy Card]]]]]
  :game.config/move-spec Card
  :game.config/match-results-view ui/match-results-view
  :game.config/starter-code starter-code/starter-code
  :game.config/test-bot (pr-str bots/random-bot)
  :game.config/seed-bots [{:code/language "clojure"
                           :code/code (pr-str bots/random-bot)}
                          {:code/language "clojure"
                           :code/code (pr-str bots/current-trophy-bot)}
                          {:code/language "clojure"
                           :code/code (pr-str bots/other-bot)}
                          {:code/language "javascript"
                           :code/code bots/js-random-bot}]})
