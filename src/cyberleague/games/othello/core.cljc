(ns cyberleague.games.othello.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.othello.engine] ;; so it gets registered
   [cyberleague.games.othello.ui :as ui]
   [cyberleague.games.othello.bots :as bots]
   [cyberleague.games.othello.helpers]
   [cyberleague.games.othello.starter-code :as starter-code]))

(def Move
  [integer?
   [:>= 0]
   [:<= 63]])

(cyberleague.game-registrar/register-game!
 {:game.config/name "othello"
  :game.config/description
  (str "Othello is a two player game played on an 8x8 board.\n"
       "The game is played with discs that are black on one side and white on the other.\n"
       "Players take turns placing a disc so that one or more of their opponents discs are enclosed between two of their own discs in a straight line.\n"
       "Play continues until there are no legal moves available for the current player.\n"
       "The player with the most discs showing their color on the board wins.")
  :game.config/rules ""
  :game.config/match-results-view ui/match-results-view
  :game.config/starter-code starter-code/starter-code
  :game.config/test-bot (pr-str bots/random-valid-bot)
  :game.config/seed-bots [{:code/language "clojure"
                           :code/code (pr-str bots/random-valid-bot)}
                          {:code/language "clojure"
                           :code/code (pr-str bots/first-valid-bot)}]
  :game.config/public-state-example {:board ["E" "E" "E" "E" "E" "E" "E" "E"
                                             "E" "E" "E" "E" "E" "E" "E" "E"
                                             "E" "E" "E" "E" "W" "E" "E" "E"
                                             "E" "E" "B" "B" "W" "E" "E" "E"
                                             "E" "E" "E" "B" "B" "E" "E" "E"
                                             "E" "E" "E" "E" "E" "B" "E" "E"
                                             "E" "E" "E" "E" "E" "E" "E" "E"
                                             "E" "E" "E" "E" "E" "E" "E" "E"]
                                     :history [{:player 277076930200589 :move 26}
                                               {:player 277076930200594 :move 20}
                                               {:player 277076930200589 :move 45}]
                                     :current-turn "W"
                                     :available-moves [34 44 25 42]
                                     :marker {277076930200589 "B" 277076930200594 "W"}}
  :game.config/internal-state-spec {:board ["E" "E" "E" "E" "E" "E" "E" "E"
                                            "E" "E" "E" "E" "E" "E" "E" "E"
                                            "E" "E" "E" "E" "W" "E" "E" "E"
                                            "E" "E" "B" "B" "W" "E" "E" "E"
                                            "E" "E" "E" "B" "B" "E" "E" "E"
                                            "E" "E" "E" "E" "E" "B" "E" "E"
                                            "E" "E" "E" "E" "E" "E" "E" "E"
                                            "E" "E" "E" "E" "E" "E" "E" "E"]
                                    :history [{:player 277076930200589 :move 26}
                                              {:player 277076930200594 :move 20}
                                              {:player 277076930200589 :move 45}]
                                    :current-turn "W"
                                    :available-moves [34 44 25 42]
                                    :marker {277076930200589 "B" 277076930200594 "W"}}

  :game.config/move-example 32

  :game.config/public-state-spec [:map
                                  [:board
                                   [:vector
                                    [:vector
                                     [:and
                                      [:enum "B" "W" "E"]
                                      [:fn (fn [v]
                                             (= (count v) 64))]]]]]
                                  [:history
                                   [:vector
                                    [:map
                                     [:player integer?]
                                     [:move Move]]]]
                                  [:available-moves
                                   [:vector
                                    [:move Move]]]]
  :game.config/move-spec Move})


