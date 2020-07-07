(ns cyberleague.test.coordinator.game-runner
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]
   [cyberleague.coordinator.game-runner :as runner]))

(deftest running-a-game-goofspiel
  (testing "can run a game"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1234
                     :bot/code-version 31
                     :bot/deployed-code (pr-str '(fn [state]
                                                   (println (state "current-trophy"))
                                                   (state "current-trophy")))}
                    {:db/id 54321
                     :bot/code-version 16
                     :bot/deployed-code
                     (pr-str '(fn [state]
                                (if (= 1 (state "current-trophy"))
                                  13
                                  (dec (state "current-trophy")))))}])]
      (is (map? result))
      (is (not (:error result)))
      (is (= 1234 (:winner result)))
      (testing "and capture output"
        (is (map? (:output result)))
        (is (not (string/blank? (get-in result [:output "bot_code_1234_run"])))))))

  (testing "can run a game with a javascript bot"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 9876
                     :bot/code-version 1
                     :bot/deployed-code (str "function (state) { "
                                             "var trophy = edn_to_json(state)[\"current-trophy\"];"
                                             "return trophy; };")
                     :bot/code {:code/language "javascript"}}
                    {:db/id 1234
                     :bot/code-version 30
                     :bot/deployed-code (pr-str '(fn [state] (state "current-trophy")))}])]
      (is (map? result))
      (is (not (:error result)))
      (is (nil? (:winner result)))))

  (testing "reports bad moves"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1235
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] (state "current-trophy")))}
                    {:db/id 54322
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] 15))}])]
      (is (= :invalid-move (:error result)))
      (is (= 0 (count (get-in result [:game-state "history"]))))
      (is (= {:bot 54322 :move 15}
             (:move result)))))

  (testing "report illegal moves"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1236
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] (state "current-trophy")))}
                    {:db/id 54323
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] 13))}])]
      (is (= :illegal-move (:error result)))
      (is (= {:bot 54323 :move 13}
             (:move result)))
      (is (= 1 (count (get-in result [:game-state "history"]))))))

  (testing "times out games that don't terminate"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1237
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] (state "current-trophy")))}
                    {:db/id 54324
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] (loop [] (recur))))}])]
      (is (= :timeout-executing (:error result))))))

(deftest using-underscore-in-javascript
  (testing "Can write a bot using underscore"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 9878
                     :bot/code-version 1
                     :bot/deployed-code (str "function (state) { "
                                             "var trophy = edn_to_json(state)[\"current-trophy\"];"
                                             "return _.identity(trophy); };")
                     :bot/code {:code/language "javascript"}}
                    {:db/id 1236
                     :bot/code-version 30
                     :bot/deployed-code (pr-str '(fn [state] (state "current-trophy")))}])]
      (is (map? result))
      (is (not (:error result)))
      (is (nil? (:winner result))))))


(deftest running-a-game-ultimate-tic-tac-toe
  (testing "can run a game"
    (let [random-bot-code
          (pr-str '(let [won-subboard (fn [board]
                                        (let [all-equal (fn [v] (and (apply = v) (first v)))]
                                          (or
                                            ; horizontal lines
                                            (all-equal (subvec board 0 3))
                                            (all-equal (subvec board 3 6))
                                            (all-equal (subvec board 6 9))
                                            ; vertical lines
                                            (all-equal (vals (select-keys board [0 3 6])))
                                            (all-equal (vals (select-keys board [1 4 7])))
                                            (all-equal (vals (select-keys board [2 5 8])))
                                            ; diagonals
                                            (all-equal (vals (select-keys board [0 4 8])))
                                            (all-equal (vals (select-keys board [2 4 6]))))))
                         board-decided? (fn [board] (or (won-subboard board) (not-any? nil? board)))]
                     (fn [{:strs [history grid] :as state}]
                       (if (empty? history)
                         ; I'm first player
                         (pr-str [2 2])
                         (let [[b sb] (get (last history) "move")
                               board-idx (if (board-decided? (grid sb))
                                           (->> (range 0 9) (remove (comp board-decided? grid)) rand-nth)
                                           sb)
                               board (grid board-idx)]
                           (pr-str
                             [board-idx
                              (->> (range 0 9)
                                   (filter (comp nil? (partial get board)))
                                   rand-nth)]))))))
          result (runner/run-game
                   {:game/name "ultimate tic-tac-toe"}
                   [{:db/id 56789
                     :bot/code-version 5
                     :bot/deployed-code random-bot-code}
                    {:db/id 98765
                     :bot/code-version 5
                     :bot/deployed-code random-bot-code}])]
      (is (map? result))
      (is (not (:error result)))))

  #_(testing "reports bad moves"
    (let [result (runner/run-game
                   {:game/name "ultimate tic-tac-toe"}
                   [{:db/id 1235
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] (state "current-trophy")))}
                    {:db/id 54322
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] 15))}])]
      (is (= :invalid-move (:error result)))
      (is (= 0 (count (get-in result [:game-state "history"]))))
      (is (= {:bot 54322 :move 15}
             (:move result)))))

  #_(testing "report illegal moves"
    (let [result (runner/run-game
                   {:game/name "goofspiel"}
                   [{:db/id 1236
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] (state "current-trophy")))}
                    {:db/id 54323
                     :bot/code-version 1
                     :bot/deployed-code (pr-str '(fn [state] 13))}])]
      (is (= :illegal-move (:error result)))
      (is (= {:bot 54323 :move 13}
             (:move result)))
      (is (= 1 (count (get-in result [:game-state "history"]))))
      )))
