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
              new-state (game/next-state g state [{12345 13 54321 10}])]
          (println new-state)
          (is (not= (:current-trophy new-state) trophy))
          (is (= 1 (count (:history new-state))))
          (is (= {12345 13 54321 10 :trophy trophy}
                 (first (:history new-state))))
          )))))
