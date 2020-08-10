(ns cyberleague.test.games.ultimate-tic-tac-toe
  (:require
   [clojure.test :refer :all]
   [cyberleague.games.ultimate-tic-tac-toe.bots :as bots]
   [cyberleague.coordinator.game-runner :as runner]))

(deftest running-a-game-ultimate-tic-tac-toe
  (testing "can run a game"
    (let [random-bot-code (pr-str bots/random-valid-bot)
          result (runner/run-game
                  {:game/name "ultimate tic-tac-toe"}
                  [{:db/id 56789
                    :bot/code {:code/code random-bot-code
                               :code/language "clojure"}}
                   {:db/id 98765
                    :bot/code {:code/code random-bot-code
                               :code/language "clojure"}}])]
      (is (map? result))
      (is (not (:error result))))))
