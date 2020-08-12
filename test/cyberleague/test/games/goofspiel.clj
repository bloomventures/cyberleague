(ns cyberleague.test.games.goofspiel
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]
   [cyberleague.games.goofspiel.bots :as bots]
   [cyberleague.coordinator.game-runner :as runner]))

(deftest running-a-game-goofspiel
  (testing "can run a game"
    (let [random-bot-code (pr-str bots/random-bot)
          result (runner/run-game
                  {:game/name "goofspiel"}
                  [{:db/id 1234
                    :bot/code {:code/code (pr-str '(fn [state] (state :current-trophy)))
                               :code/language "clojure"}}
                   {:db/id 54321
                    :bot/code-version 16
                    :bot/code {:code/code (pr-str '(fn [state] (if (= (state :current-trophy) 1)
                                                                 13
                                                                 (dec (state :current-trophy)))))
                               :code/language "clojure"}}])]
      (is (map? result))
      (is (not (:error result)))
      (is (= 1234 (:winner result)))
      #_(testing "and capture output"
          (is (map? (:output result)))
          (is (not (string/blank? (get-in result [:output "bot_code_1234_run"])))))))

  (testing "reports bad moves"
    (let [result (runner/run-game
                  {:game/name "goofspiel"}
                  [{:db/id 1235
                    :bot/code {:code/code (pr-str '(fn [state] (state :current-trophy)))
                               :code/language "clojure"}}
                   {:db/id 54322
                    :bot/code {:code/code (pr-str '(fn [state] 15))
                               :code/language "clojure"}}])]
      (is (= :invalid-move (:error result)))
      (is (= 0 (count (get-in result [:game-state :history]))))
      (is (= {:bot 54322 :move 15}
             (:move result)))))

  (testing "report illegal moves"
    (let [result (runner/run-game
                  {:game/name "goofspiel"}
                  [{:db/id 1236
                    :bot/code {:code/code (pr-str '(fn [state] (state :current-trophy)))
                               :code/language "clojure"}}
                   {:db/id 54323
                    :bot/code {:code/code (pr-str '(fn [state] 13))
                               :code/language "clojure"}}])]
      (is (= :illegal-move (:error result)))
      (is (= {:bot 54323 :move 13}
             (:move result)))
      (is (= 1 (count (get-in result [:game-state :history]))))))

  #_(testing "times out games that don't terminate"
      (let [result (runner/run-game
                    {:game/name "goofspiel"}
                    [{:db/id 1237
                      :bot/code-version 1
                      :bot/deployed-code (pr-str '(fn [state] (state :current-trophy)))}
                     {:db/id 54324
                      :bot/code-version 1
                      :bot/deployed-code (pr-str '(fn [state] (loop [] (recur))))}])]
        (is (= :timeout-executing (:error result))))))
