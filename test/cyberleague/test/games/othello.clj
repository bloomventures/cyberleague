(ns cyberleague.test.games.othello
  (:require
    [clojure.test :refer :all]
    [cyberleague.games.othello.bots :as bots]
    [cyberleague.games.othello.helpers :as othello]
    [cyberleague.coordinator.game-runner :as runner]
    [cyberleague.games.othello.engine]
    [cyberleague.games.protocol :as game]))

(deftest othello-engine
  (testing "Can create an engine"
    (is (satisfies? game/IGameEngine (game/make-engine {:game/name "othello"}))))

  (testing "basic game setup"
    (let [g (game/make-engine {:game/name "othello"})]
      (is (not (game/simultaneous-turns? g)))
      (is (= 2 (game/number-of-players g)))
      (is (game/valid-move? g 26))
      (is (game/valid-move? g 44))
      (is (game/valid-move? g 19))
      (is (not (game/valid-move? g "x27")))
      (is (not (game/valid-move? g :bob)))

      (let [state {:board othello/initial-board
                   :available-moves [26 19 44 37]
                   :current-turn "B"}]
        (is (game/legal-move? g state 123 26))
        (is (not (game/legal-move? g state 123 36)))
        (is (not (game/legal-move? g state 123 45)))
        (is (not (game/legal-move? g state 123 17))))))

  (testing "progressing game state"
    (let [g (game/make-engine {:game/name "othello"})
          state (game/init-state g [123 456])]

      (testing "starting with a good state"
        (is (zero? (count (state :history))))
        (testing "can play legal moves on first move"
          (is (game/legal-move? g state 123 26))
          (is (game/legal-move? g state 123 19))
          (is (game/legal-move? g state 123 37))
          (is (game/legal-move? g state 123 44))))

      (testing "progressing the state with moves"
        (let [new-state (game/next-state g state {123 19})]
          (is (= "B" (get-in new-state [:board 27])))
          (is (= "B" (get-in new-state [:board 28])))
          (is (= "B" (get-in new-state [:board 35])))
          (is (= "B" (get-in new-state [:board 19])))
          (is (not (= "B" (get-in new-state [:board 36]))))
          (is (= 1 (count (new-state :history))))
          (is (= {:player 123 :move 19} (first (new-state :history))))
          (is (not (game/game-over? g new-state)))
          (is (not (game/legal-move? g new-state 456 35)))
          (is (game/legal-move? g new-state 456 34))

          (let [third-state (game/next-state g new-state {456 34})]
            (is (= 2 (count (third-state :history))))
            (is (= "W" (get-in third-state [:board 35])))
            (is (not (game/game-over? g third-state))))))))

  (testing "change stone colours in 2 directions"
    (let [g (game/make-engine {:game/name "othello"})
          first-state (-> (game/init-state g [123 456])
                          (assoc-in [:board  43] "W")
                          (assoc-in [:board  42] "B"))]
      (is (not (game/game-over? g first-state)))
      (is (game/legal-move? g first-state 123 44))
      (let [second-state (game/next-state g first-state {123 44})]
        (is (= "B" (get-in second-state [:board 44])))
        (is (= "B" (get-in second-state [:board 36])))
        (is (= "B" (get-in second-state [:board 43])))
        (is (= "B" (get-in second-state [:board 28])))
        (is (= "W" (get-in second-state [:board 27]))))))

  (testing "change a chain"
    (let [g (game/make-engine {:game/name "othello"})
          first-state (-> (game/init-state g [123 456])
                          (assoc-in [:board 19] "W"))
          second-state (game/next-state g first-state {123 11})]
      (is (= "B" (get-in second-state [:board 19])))
      (is (= "B" (get-in second-state [:board 27])))))

  #_(testing "can finish game"
    (let [g (game/make-engine {:game/name "othello"})
          first-state (game/init-state g [123 456])
          second-state (->> (range 64)
                            (map (fn [curr]
                                   (assoc-in first-state [:board curr] "B"))))]
      (is (game/game-over? g second-state))
      (let [third-state
            (-> first-state
                (assoc-in [:board  27] "B")
                (assoc-in [:board 36] "B"))]
        (is (game/game-over? g third-state))
        (is (= "B" (game/winner g third-state)))))))

(deftest running-a-game-othello
  (testing "can run a game"
    (let [random-bot-code (pr-str bots/random-valid-bot)
          first-bot-code (pr-str bots/first-valid-bot)
          result (runner/run-game
                   {:game/name "othello"}
                   [{:db/id 12345
                     :bot/code {:code/code random-bot-code
                                :code/language "clojure"}}
                    {:db/id 56789
                     :bot/code {:code/code first-bot-code
                                :code/language "clojure"}}])] (is (map? result))
         (println "Result" result)
      (is (not (:error result))))))
