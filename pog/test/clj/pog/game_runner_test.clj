(ns pog.game-runner-test
  (:require [clojure.test :refer :all]
            [pog.game-runner :as runner]))


(deftest running-a-game
  (testing "can run a game"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1234
                     :bot/code-version 28
                     :bot/deployed-code '(fn [state] (state "current-trophy"))}
                    {:db/id 54321
                     :bot/code-version 15
                     :bot/deployed-code '(fn [state]
                                           (if (= 1 (state "current-trophy"))
                                             13
                                             (dec (state "current-trophy"))))}])]
      (is (map? result))
      (is (not (:error result)))
      (is (= 1234 (:winner result)))
      )))
