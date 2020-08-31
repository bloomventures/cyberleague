(ns cyberleague.games.ultimate-tic-tac-toe.core
  (:require
   [cyberleague.game-registrar]
   [cyberleague.games.ultimate-tic-tac-toe.engine] ;; so it gets registered
   [cyberleague.games.ultimate-tic-tac-toe.ui :as ui]
   [cyberleague.games.ultimate-tic-tac-toe.bots :as bots]
   [cyberleague.games.ultimate-tic-tac-toe.starter-code :as starter-code]))

(def Move
  [:vector
   [:and
    integer?
    [:>= 0]
    [:<= 8]
    [:fn (fn [v]
           (= 2 (count v)))]]])

(cyberleague.game-registrar/register-game!
 {:game.config/name "ultimate tic-tac-toe"
  :game.config/description
  (str "You mastered Tic-Tac-Toe in minutes, but let's see how long it will take you to master it's bigger brother.\n"
       "In Ultimate Tic-Tac-Toe, you play 9 games of Tic-Tac-Toe nested a meta Tic-Tac-Toe game.")
  :game.config/rules ""
  :game.config/match-results-view ui/match-results-view
  :game.config/match-results-styles ui/>results-styles
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
  :game.config/move-example [0 1]
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
  :game.config/move-spec Move
  :game.config/starter-code starter-code/starter-code
  :game.config/test-bot (pr-str bots/random-valid-bot)
  :game.config/seed-bots [{:code/language "clojure"
                           :code/code (pr-str bots/random-valid-bot)}
                          {:code/language "clojure"
                           :code/code (pr-str bots/first-valid-bot)}]})
