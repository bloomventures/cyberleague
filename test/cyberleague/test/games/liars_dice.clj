(ns cyberleague.test.games.liars-dice
  (:require
   [clojure.test :refer :all]
   [cyberleague.games.liars-dice.engine]
   [cyberleague.games.protocol :as game]))

(def slug "liars-dice")
(def players [0 1])

(defn engine []
  (game/make-engine {:game/slug slug}))

(deftest engine-creation
  (testing "Can create an engine"
    (is (satisfies? game/IGameEngine (engine)))))

(deftest basic-properties
  (let [g (engine)]
    (testing "not simultaneous"
      (is (not (game/simultaneous-turns? g))))

    (testing "two players"
      (is (= 2 (game/number-of-players g))))

    (testing "valid-move? on bids"
      (is (game/valid-move? g {:action "bid" :quantity 1 :face 1}))
      (is (game/valid-move? g {:action "bid" :quantity 3 :face 6}))
      (is (not (game/valid-move? g {:action "bid" :quantity 0 :face 3})))
      (is (not (game/valid-move? g {:action "bid" :quantity 1 :face 7})))
      (is (not (game/valid-move? g {:action "bid" :quantity 1 :face 0}))))

    (testing "valid-move? on challenge"
      (is (game/valid-move? g {:action "challenge"})))

    (testing "valid-move? rejects garbage"
      (is (not (game/valid-move? g "challenge")))
      (is (not (game/valid-move? g {:action "nonsense"})))
      (is (not (game/valid-move? g nil))))))

(deftest init-state-test
  (let [g (engine)
        state (game/init-state g players)]

    (testing "each player starts with 5 dice"
      (is (= 5 (count (get-in state [:dice 0]))))
      (is (= 5 (count (get-in state [:dice 1])))))

    (testing "hands contain valid die faces"
      (is (every? #(<= 1 % 6) (get-in state [:dice 0])))
      (is (every? #(<= 1 % 6) (get-in state [:dice 1]))))

    (testing "history starts empty"
      (is (empty? (:history state))))

    (testing "not game over"
      (is (not (game/game-over? g state))))))

(deftest legal-move-test
  (let [g (engine)
        state (game/init-state g players)]

    (testing "first bid: any bid is legal for player 0"
      (is (game/legal-move? g state 0 {:action "bid" :quantity 1 :face 3})))

    (testing "challenge is illegal with no bids yet"
      (is (not (game/legal-move? g state 0 {:action "challenge"}))))

    (testing "player 1 cannot move before player 0"
      (is (not (game/legal-move? g state 1 {:action "bid" :quantity 1 :face 3}))))

    (testing "after a bid, other player can bid higher or challenge"
      (let [state2 (game/next-state g state {0 {:action "bid" :quantity 2 :face 4}})]
        (is (game/legal-move? g state2 1 {:action "challenge"}))
        (is (game/legal-move? g state2 1 {:action "bid" :quantity 3 :face 4}))
        (is (game/legal-move? g state2 1 {:action "bid" :quantity 2 :face 5}))
        (is (not (game/legal-move? g state2 1 {:action "bid" :quantity 2 :face 4})))
        (is (not (game/legal-move? g state2 1 {:action "bid" :quantity 1 :face 4})))
        (is (not (game/legal-move? g state2 1 {:action "bid" :quantity 2 :face 3})))))))

(deftest bid-progression
  (let [g (engine)
        state (game/init-state g players)
        state1 (game/next-state g state {0 {:action "bid" :quantity 2 :face 3}})]

    (testing "history records the bid with player-id"
      (is (= [{:player-id 0 :move {:action "bid" :quantity 2 :face 3}}]
             (:history state1))))

    (let [state2 (game/next-state g state1 {1 {:action "bid" :quantity 3 :face 3}})]
      (testing "second bid appended"
        (is (= 2 (count (:history state2))))
        (is (= {:player-id 1 :move {:action "bid" :quantity 3 :face 3}}
               (last (:history state2))))))))

(deftest challenge-invalid-bid
  (let [g (engine)
        state {:dice {0 [2 3 4] 1 [3 4 5]}
               :history [{:player-id 0 :move {:action "bid" :quantity 5 :face 6}}]}
        state2 (game/next-state g state {1 {:action "challenge"}})]

    (testing "challenge is appended to history"
      (is (= {:player-id 1 :move {:action "challenge"}} (last (:history state2)))))

    (testing "challenger wins when bid is invalid"
      (is (= 1 (game/winner g state2))))

    (testing "game is over"
      (is (game/game-over? g state2)))))

(deftest challenge-valid-bid
  (let [g (engine)
        state {:dice {0 [2 2 2] 1 [2 2]}
               :history [{:player-id 0 :move {:action "bid" :quantity 5 :face 2}}]}
        state2 (game/next-state g state {1 {:action "challenge"}})]

    (testing "bidder wins when bid is valid"
      (is (= 0 (game/winner g state2))))

    (testing "game is over"
      (is (game/game-over? g state2)))))

(deftest wild-ace-counting
  (let [g (engine)
        state {:dice {0 [1 1 3] 1 [4 1]}
               :history [{:player-id 0 :move {:action "bid" :quantity 4 :face 4}}]}
        state2 (game/next-state g state {1 {:action "challenge"}})]
    (testing "aces count as wild — bid of 4×4 is valid (1 four + 3 aces = 4)"
      (is (= 0 (game/winner g state2))))))

(deftest ace-bid
  (let [g (engine)
        state {:dice {0 [1 2] 1 [1 3]}
               :history [{:player-id 0 :move {:action "bid" :quantity 2 :face 1}}]}
        state2 (game/next-state g state {1 {:action "challenge"}})]
    (testing "bidding on 1s: all 1s count"
      (is (= 0 (game/winner g state2))))))

(deftest anonymize-state-for-test
  (let [g (engine)
        state {:dice {0 [1 2 3 4 5] 1 [2 3 4 5 6]}
               :history [{:player-id 0 :move {:action "bid" :quantity 2 :face 3}}
                         {:player-id 1 :move {:action "bid" :quantity 3 :face 4}}]}
        ctx0 (game/anonymize-state-for g 0 state)
        ctx1 (game/anonymize-state-for g 1 state)]

    (testing "each player sees their own id"
      (is (= 0 (:my-id ctx0)))
      (is (= 1 (:my-id ctx1))))

    (testing "each player sees only their own dice"
      (is (= [1 2 3 4 5] (:my-dice ctx0)))
      (is (= [2 3 4 5 6] (:my-dice ctx1))))

    (testing "history is passed through unchanged"
      (is (= (:history state) (:history ctx0)))
      (is (= (:history state) (:history ctx1))))))
