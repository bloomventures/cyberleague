(ns cyberleague.test.coordinator.db
  (:require
   [clojure.test :refer :all]
   [cyberleague.db.core :as db]))

(use-fixtures :each
              (fn [t]
                (binding [db/*uri* "datomic:mem://cyberleague-testing"]
                  (db/init)
                  (db/with-conn (t))
                  (datomic.api/delete-database db/*uri*))))

(deftest user-crud
  (testing "Can create a user"
    (let [new-user (db/create-user 123 "name")]
      (is (= "name" (:user/name new-user)))
      (is (= 123 (:user/gh-id new-user)))

      (testing "and get user"
        (is (= new-user (db/get-user (:db/id new-user))))))))

(deftest games-crud
  (testing "Can create a game"
    (let [game1 (db/create-game "goofenspiel"
                                "simple game descr")]

      (is (= "goofenspiel" (:game/name game1)))
      (is (= "simple game descr" (:game/description game1)))

      (testing "and get a game"
        (let [game1get (db/get-game (:db/id game1))]
          (is (= "goofenspiel" (:game/name game1get)))
          (is (= "simple game descr" (:game/description game1get)))))

      (testing "and get all games"
        (let [game2 (db/create-game "super-tic-tac-toe"
                                    "tic-tac-toe, but more so")]
          (is (= #{game1 game2} (set (db/get-games)))))))))

(deftest bots-crud
  (let [user (db/create-user 123 "james")
        game (db/create-game "goofenspiel"
                             "simple game descr")]
   (testing "can create bot"
     (let [bot (db/create-bot (:db/id user) (:db/id game))]
       (is (= user (:bot/user bot)))
       (is (= game (:bot/game bot)))
       (is (nil? (:bot/code bot)))

       (testing "and add code"
         (let [bot-with-code (db/update-bot-code (:db/id bot) "(fn [] 1)")]
           (is (= (get-in bot-with-code [:bot/code :code/code]  "(fn [] 1)")))
           (let [updated-code (db/update-bot-code (:db/id bot) "(fn [] 2)")]
             (is (= (get-in updated-code [:bot/code :code/code]  "(fn [] 2)")))))

         (testing "and see history of code"
           (let [_ (db/update-bot-code (:db/id bot) "(fn [] 3)")
                 history (db/code-history (:db/id bot))]
             (is (= ["(fn [] 1)" "(fn [] 2)" "(fn [] 3)"]
                    (mapv first history))))

           (testing "and set the current version of the code"
             (db/deploy-bot (:db/id bot))
             (db/update-bot-code (:db/id bot) "(fn [] 4)")
             (is (= (pr-str '(fn [] 3)) (db/deployed-code (:db/id bot))))
             (is (= "(fn [] 4)" (get-in (db/by-id (:db/id bot)) [:bot/code :code/code]))))))

       (testing "and get game bots"
         (is (= (db/get-game-bots (:db/id game)) [bot])))

       (testing "and get user bots"
         (is (= (db/get-user-bots (:db/id user)) [bot])))))))


#_(deftest matches
  (testing "can get match"
    (let [match (db/create-match {})
          match-get (db/get-match (:id match))]

      (is false))))
