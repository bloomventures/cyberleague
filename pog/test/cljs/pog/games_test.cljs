(ns pog.games-test
  (:require-macros [cemerick.cljs.test :refer [is deftest with-test run-tests testing test-var]])
  (:require [cemerick.cljs.test :as t]
            [pog.games :as game]))

(deftest goofspiel-engine
  (testing "Can create an engine"
    (is (satisfies? game/IGameEngine (game/make-engine {:game/name "goofspiel"}))))
  (testing "does basic info correctly"
    (let [g (game/make-engine {:game/name "goofspiel"})]
      (is (game/simultaneous-turns? g))

      (is (game/valid-move? g 5))
      (is (game/valid-move? g 1))
      (is (game/valid-move? g 13))
      (is (not (game/valid-move? g 0)))
      (is (not (game/valid-move? g 14)))

      (let [state {"player-cards" {12345 #{1 2 3 4 5}}}]
        (is (game/legal-move? g state 12345 3))
        (is (not (game/legal-move? g state 12345 13)))
        (is (not (game/legal-move? g state 12345 6))))))

  (testing "progressing game state"
    (let [g (game/make-engine {:game/name "goofspiel"})
          state (game/init-state g [12345 54321])]

      (testing "starting with a good state"
        (is (<= 1 (state "current-trophy") 13))
        (is (not (contains? (state "trophy-cards" ) (state "current-trophy"))))
        (is (zero? (count (state "history")))))

      (testing "progressing the state with moves"
        (let [trophy (state "current-trophy")
              new-state (game/next-state g state {12345 13 54321 10})]
          (is (not= (new-state "current-trophy") trophy))
          (is (= 1 (count (new-state "history"))))
          (is (= {12345 13 54321 10 "trophy" trophy}
                 (first (new-state "history"))))
          (is (not (contains? (get-in new-state ["player-cards" 12345]) 13)))
          (is (not (contains? (get-in new-state ["player-cards" 54321]) 10)))
          (is (not (game/game-over? g new-state)))
          (let [third-state (game/next-state g new-state {12345 12 54321 9})]
            (is (= 2 (count (third-state "history"))))
            (is (not (game/game-over? g third-state))))))))

  (testing "Finishing game"
    (let [g (game/make-engine {:game/name "goofspiel"})
          almost-done-state {"player-cards" {12345 #{1}
                                            54321 #{13}}
                             "trophy-cards" #{}
                             "current-trophy" 1
                             "history" (vec (for [i (range 13 1 -1)]
                                             {12345 i 54321 (dec i) "trophy" i}))}]
      (is (not (game/game-over? g almost-done-state)))
      (let [done-state (game/next-state g almost-done-state {12345 1 54321 13})]
        (is (game/game-over? g done-state))
        (is (= 12345 (game/winner g done-state)))
        ))))

(deftest clojurescript-misc

  (deftype GameException [info]
    Object
    (getInfo [this] info))

  (is (= {:a 1 :x 2}
         (try
           (throw (GameException. {:a 1 :x 2}))
           (catch GameException e
             (.getInfo e))))))

(defn make-grid []
  (vec (repeat 9 (vec (repeat 9 nil)))))

(deftest ultimate-tic-tac-toe-engine
  (testing "Can create an engine"
    (is (satisfies? game/IGameEngine (game/make-engine {:game/name "ultimate tic-tac-toe"}))))
  (testing "board helper"
    (is (not (game/won-subboard (vec (repeat 9 nil)))))
    (is (not (game/won-subboard ["x" "o" "o"
                                 "o" "x" "x"
                                 "x" "o" "o"])))
    (is (game/board-decided? ["x" "o" "o"
                              "o" "x" "x"
                              "x" "o" "o"]))
    (is (not (game/won-subboard ["x" "o" "o"
                                 "o" nil "x"
                                 "x" "o" "o"])))
    (is (= "x" (game/won-subboard ["x" "o" "o"
                                   "x" "x" "o"
                                   "x" "o" "x"])))
    (is (= "x" (game/won-subboard ["x" "x" "o"
                                   "o" "x" "o"
                                   "o" "x" "x"])))
    (is (= "o" (game/won-subboard ["x" "x" "o"
                                   "o" "o" "x"
                                   "o" "x" "x"]))))
  (testing "does basic info correctly"
    (let [g (game/make-engine {:game/name "ultimate tic-tac-toe"})]
      (is (not (game/simultaneous-turns? g)))

      (is (game/valid-move? g [0 0]))
      (is (game/valid-move? g [8 8]))
      (is (game/valid-move? g [5 8]))
      (is (not (game/valid-move? g [0])))
      (is (not (game/valid-move? g [9 1])))
      (is (not (game/valid-move? g [1 9])))

      (testing "legal moves"
        (testing "must play on the board corresponding to cell of last opponent move"
          (let [state {"grid" (assoc-in (make-grid) [1 2] "x")
                       "history" [{"player" 54321 "move" [1 2]}]}]
            (is (game/legal-move? g state 12345 [2 8]))
            (is (game/legal-move? g state 12345 [2 0]))
            (is (not (game/legal-move? g state 12345 [1 1])))
            (is (not (game/legal-move? g state 12345 [5 5])))))

        (testing "can play anywhere if sent to a full board"
          (let [state {"grid" (assoc-in (make-grid) [2] (vec (repeat 9 "x")))
                       "history" [{"player" 54321 "move" [1 2]}]}]
            (is (not (game/legal-move? g state 12345 [2 8])))
            (is (not (game/legal-move? g state 12345 [2 0])))
            (is (game/legal-move? g state 12345 [1 1]))
            (is (game/legal-move? g state 12345 [5 5]))
            (is (game/legal-move? g state 12345 [8 8]))
            ))

        (testing "or won board"
          (let [state {"grid" (-> (make-grid)
                                  (assoc 0 (vec (concat (repeat 3 "x") (repeat 6 nil)))))
                       "history" [{"player" 54321 "move" [2 0]}]}]
            (is (not (game/legal-move? g state 12345 [0 8])))
            (is (not (game/legal-move? g state 12345 [0 0])))
            (is (game/legal-move? g state 12345 [1 1]))
            (is (game/legal-move? g state 12345 [5 5]))
            (is (game/legal-move? g state 12345 [8 8]))
            (is (game/legal-move? g state 12345 [2 8]))
            (is (game/legal-move? g state 12345 [2 0]))
            )))))

  (testing "progressing game state"
    (let [g (game/make-engine {:game/name "ultimate tic-tac-toe"})
          state (game/init-state g [12345 54321])]

      (testing "starting with a good state"
        (is (every? (partial every? nil?) (state "grid")))
        (is (zero? (count (state "history"))))
        (testing "can play anywhere on the first move"
          (is (game/legal-move? g state 12345 [2 8]))
          (is (game/legal-move? g state 12345 [2 0]))
          (is (game/legal-move? g state 12345 [1 1]))
          (is (game/legal-move? g state 12345 [5 5]))
          (is (game/legal-move? g state 12345 [8 8]))))

      (testing "progressing the state with moves"
        (let [new-state (game/next-state g state {12345 [0 5]})]
          (is (= "x" (get-in new-state ["grid" 0 5])))
          (is (= 1 (count (new-state "history"))))
          (is (= {"player" 12345 "move" [0 5]} (first (new-state "history"))))
          (is (not (game/game-over? g new-state)))

          (is (not (game/legal-move? g new-state 54321 [0 1])))
          (is (not (game/legal-move? g new-state 54321 [6 0])))
          (is (game/legal-move? g new-state 54321 [5 3]))
          (let [third-state (game/next-state g new-state {54321 [5 3]})]
            (is (= 2 (count (third-state "history"))))
            (is (= "o" (get-in third-state ["grid" 5 3])))
            (is (not (game/game-over? g third-state))))))))

  (testing "Finishing game"
    (testing "with a simple winning board"
      (let [g (game/make-engine {:game/name "ultimate tic-tac-toe"})
            almost-done-state (-> (game/init-state g [12345 54321])
                                  (assoc-in ["grid" 0] (vec (concat (repeat 3 "x") (repeat 6 nil))))
                                  (assoc-in ["grid" 1] (vec (concat (repeat 3 "x") (repeat 6 nil))))
                                  (assoc-in ["grid" 2] (vec (concat (repeat 2 "x") (repeat 7 nil)))))]
        (is (not (game/game-over? g almost-done-state)))
        (let [done-state (game/next-state g almost-done-state {12345 [2 2]})]
          (is (game/game-over? g done-state))
          (is (= 12345 (game/winner g done-state))))))))
