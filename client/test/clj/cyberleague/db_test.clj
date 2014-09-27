(ns cyberleague.db-test
  (:require [clojure.test :refer :all]
            [cyberleague.db :as db]))

(use-fixtures :each
              (fn [t]
                (binding [db/*uri* "datomic:mem://testing"]
                  (db/init)
                  (db/with-conn (t))
                  (datomic.api/delete-database db/*uri*))))

(deftest database-stuff
  (testing "Can create a user"
    (let [new-user (db/create-user "token" "name")]
      (is (= "name" (:user/name new-user)))
      (is (= "token" (:user/token new-user)))
      (is (= new-user (db/by-id (:db/id new-user))))
      )))
