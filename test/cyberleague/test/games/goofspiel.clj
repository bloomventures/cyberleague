(ns cyberleague.test.games.goofspiel
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]
   [cyberleague.games.protocol :as game]
   [cyberleague.games.goofspiel.bots :as bots]
   [cyberleague.coordinator.game-runner :as runner]))

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
          state (game/init-state g [12345 54321])]

      (testing "starting with a good state"
        (is (<= 1 (state :current-trophy) 13))
        (is (not (contains? (state :trophy-cards) (state :current-trophy))))
        (is (zero? (count (state :history)))))

      (testing "progressing the state with moves"
        (let [trophy (state :current-trophy)
              new-state (game/next-state g state {12345 13 54321 10})]
          (is (not= (new-state :current-trophy) trophy))
          (is (= 1 (count (new-state :history))))
          (is (= {12345 13 54321 10 :trophy trophy}
                 (first (new-state :history))))
          (is (not (contains? (get-in new-state [:player-cards 12345]) 13)))
          (is (not (contains? (get-in new-state [:player-cards 54321]) 10)))
          (is (not (game/game-over? g new-state)))
          (let [third-state (game/next-state g new-state {12345 12 54321 9})]
            (is (= 2 (count (third-state :history))))
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
        (is (= 12345 (game/winner g done-state)))))))

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
