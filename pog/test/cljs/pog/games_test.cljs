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

      (let [state {:player-cards {12345 #{1 2 3 4 5}}}]
        (is (game/legal-move? g state 12345 3))
        (is (not (game/legal-move? g state 12345 13)))
        (is (not (game/legal-move? g state 12345 6))))))

  (testing "progressing game state"
    (let [g (game/make-engine {:game/name "goofspiel"})
          state (game/init-state g 12345 54321)]

      (testing "starting with a good state"
        (is (<= 1 (:current-trophy state) 13))
        (is (not (contains? (:trophy-cards state) (:current-trophy state))))
        (is (zero? (count (:history state)))))

      (testing "progressing the state with moves"
        (let [trophy (:current-trophy state)
              new-state (game/next-state g state {12345 13 54321 10})]
          (is (not= (:current-trophy new-state) trophy))
          (is (= 1 (count (:history new-state))))
          (is (= {12345 13 54321 10 :trophy trophy}
                 (first (:history new-state))))
          (is (not (contains? (get-in new-state [:player-cards 12345]) 13)))
          (is (not (contains? (get-in new-state [:player-cards 54321]) 10)))
          (is (not (game/game-over? g new-state)))
          (let [third-state (game/next-state g new-state {12345 12 54321 9})]
            (is (= 2 (count (:history third-state))))
            (is (not (game/game-over? g third-state))))))))

  (testing "Finishing game"
    (let [g (game/make-engine {:game/name "goofspiel"})
          almost-done-state {:player-cards {12345 #{1}
                                            54321 #{13}}
                             :trophy-cards #{}
                             :current-trophy 1
                             :history (vec (for [i (range 13 1 -1)]
                                             {12345 i 54321 (dec i) :trophy i}))}]
      (is (not (game/game-over? g almost-done-state)))
      (let [done-state (game/next-state g almost-done-state {12345 1 54321 13})]
        (is (game/game-over? g done-state))
        ))))
