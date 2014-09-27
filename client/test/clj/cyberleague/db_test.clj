(ns cyberleague.db-test
  (:require [clojure.test :refer :all]
            [cyberleague.db :as db]))

(use-fixtures :each
              (fn [t]
                (binding [db/*uri* "datomic:mem://testing"]
                  (db/init)
                  (db/with-conn (t))
                  (datomic.api/delete-database db/*uri*))))

(deftest user-crud
  (testing "Can create a user"
    (let [new-user (db/create-user "token" "name")]
      (is (= "name" (:user/name new-user)))
      (is (= "token" (:user/token new-user)))
      (is (= new-user (db/by-id (:db/id new-user))))
      )))

(deftest games-crud
  (testing "Can create games"
    (let [game1 (db/create-game "goofenspiel"
                                "simple game descr"
                                "1. ....
                                2. ....")
          game2 (db/create-game "super-tic-tac-toe"
                                "tic-tac-toe, but more so"
                                "bloop blaap balaz")]
      (is (= "goofenspiel" (:game/name game1)))
      (is (= "bloop blaap balaz" (:game/rules game2)))
      (is (= #{game1 game2} (set (db/games))))
      )))

(deftest bots-crud
  (let [user (db/create-user "aaa" "james")
        game (db/create-game "goofenspiel"
                             "simple game descr"
                             "1. ....
                             2. ....")]
   (testing "can create bots"
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
             (is (= "(fn [] 3)" (db/deployed-code (:db/id bot))))
             (is (= "(fn [] 4)" (get-in (db/by-id (:db/id bot)) [:bot/code :code/code])))
             )))))))
