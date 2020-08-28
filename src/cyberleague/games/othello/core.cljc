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
  (str "Othello")
  :game.config/rules ""
  :game.config/match-results-view ui/match-results-view
  :game.config/match-results-styles ui/>results-styles
  :game.config/starter-code starter-code/starter-code
  :game.config/test-bot (pr-str bots/random-valid-bot)
  :game.config/seed-bots [(pr-str bots/random-valid-bot)
                          (pr-str bots/first-valid-bot)]
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


