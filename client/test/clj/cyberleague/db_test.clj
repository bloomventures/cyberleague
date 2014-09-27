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
