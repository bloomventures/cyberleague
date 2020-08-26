(ns cyberleague.games.othello.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.othello.engine] ;; so it gets registered
   [cyberleague.games.othello.ui :as ui]
   [cyberleague.games.othello.bots :as bots]
   [cyberleague.games.othello.helpers]
   [cyberleague.games.othello.starter-code :as starter-code]))

(def Move
  [:vector
   [:and
    integer?
    [:>= 0]
    [:<= 8]
    [:fn (fn [v]
           (= 2 (count v)))]]])

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
  :game.config/public-state-example {:grid [["x" "o" nil nil nil nil nil nil nil]
                                            (vector (repeat 9 nil))
                                            (vector (repeat 9 nil))
                                            (vector (repeat 9 nil))
                                            (vector (repeat 9 nil))
                                            (vector (repeat 9 nil))
                                            (vector (repeat 9 nil))
                                            (vector (repeat 9 nil))
                                            (vector (repeat 9 nil))]
                                     :history [{:player 1234 :move [0 0]}
                                               {:player 4567 :move [0 1]}]}
    :game.config/internal-state-spec {:grid [["x" "o" nil nil nil nil nil nil nil]
                                           (vector (repeat 9 nil))
                                           (vector (repeat 9 nil))
                                           (vector (repeat 9 nil))
                                           (vector (repeat 9 nil))
                                           (vector (repeat 9 nil))
                                           (vector (repeat 9 nil))
                                           (vector (repeat 9 nil))
                                           (vector (repeat 9 nil))]
                                    :history [{:player 1234 :move [0 0]}
                                              {:player 4567 :move [0 1]}]}

  :game.config/move-example 32

  :game.config/public-state-spec [:map
                                  [:grid
                                   [:vector
                                    [:and
                                     [:vector
                                      [:and
                                       [:enum "x" "o" nil]
                                       [:fn (fn [v]
                                              (= (count v) 9))]]]
                                     [:fn (fn [v]
                                            (= (count v) 9))]]]]
                                  [:history
                                   [:vector
                                    [:map
                                     [:player integer?]
                                     [:move Move]]]]]
  :game.config/move-spec Move})


