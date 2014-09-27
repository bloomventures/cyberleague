(ns pog.game-runner-test
  (:require [clojure.test :refer :all]
            [pog.game-runner :as runner]))


(deftest running-a-game
  (testing "can run a game"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1234
                     :bot/code-version 12
                     :bot/deployed-code '(fn [state]
                                           (:current-trophy state))}
                    {:db/id 54321
                     :bot/code-version 4
                     :bot/deployed-code '(fn [state]
                                           (if (= 1 (:current-trophy state))
                                             13
                                             (dec (:current-trophy state))))}])]
      (println result)
      (is (map? result))
      (is (not (:error result)))
      (is (= 1234 (:winner result)))
      )))
