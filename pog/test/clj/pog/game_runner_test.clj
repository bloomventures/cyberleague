(ns pog.game-runner-test
  (:require [clojure.test :refer :all]
            [pog.game-runner :as runner]))


(deftest running-a-game
  (testing "can run a game"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1234
                     :bot/code-version 30
                     :bot/deployed-code '(fn [state] (state "current-trophy"))}
                    {:db/id 54321
                     :bot/code-version 16
                     :bot/deployed-code '(fn [state]
                                           (if (= 1 (state "current-trophy"))
                                             13
                                             (dec (state "current-trophy"))))}])]
      (is (map? result))
      (is (not (:error result)))
      (is (= 1234 (:winner result)))))

  (testing "reports bad moves"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1235
                     :bot/code-version 1
                     :bot/deployed-code '(fn [state] (state "current-trophy"))}
                    {:db/id 54322
                     :bot/code-version 1
                     :bot/deployed-code '(fn [state] 15)}])]
      (is (= :invalid-move (:error result)))
      (is (= 0 (count (get-in result [:game-state "history"]))))
      (is (= {:bot 54322 :move 15}
             (:move result)))))

  (testing "report illeagl moves"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1236
                     :bot/code-version 1
                     :bot/deployed-code '(fn [state] (state "current-trophy"))}
                    {:db/id 54323
                     :bot/code-version 1
                     :bot/deployed-code '(fn [state] 13)}])]
      (is (= :illegal-move (:error result)))
      (is (= {:bot 54323 :move 13}
             (:move result)))
      (is (= 1 (count (get-in result [:game-state "history"]))))
      )
    ))
