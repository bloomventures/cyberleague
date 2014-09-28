(ns cyberleague.handler-test
  (:require [clojure.test :refer :all]
            [cyberleague.db :as db]
            [clojure.tools.reader.edn :as edn]
            [cyberleague.handler :refer [app] :as handler]))

(use-fixtures :each
              (fn [t]
                (binding [db/*uri* "datomic:mem://testing"]
                  (db/init)
                  (t)
                  (datomic.api/delete-database db/*uri*))))

(defn edn-request [web-app method url & data]
  (let [response (web-app {:request-method method
                           :headers {"Accept" "application/edn"}
                           :uri url })]
    (assoc response :body (edn/read-string (response :body)))))


(deftest routes
  (let [user-1 (db/with-conn (db/create-user 123 "alice"))
        user-2 (db/with-conn (db/create-user 456 "bob"))
        game-1 (db/with-conn (db/create-game "a" "foobar"))
        game-2 (db/with-conn (db/create-game "b" "bazbonk"))
        bot-u1g1 (db/with-conn (db/create-bot (:db/id user-1) (:db/id game-1)))
        _ (db/with-conn (db/update-bot-code (:db/id bot-u1g1) "(fn [] false)"))
        _ (db/with-conn (db/deploy-bot (:db/id bot-u1g1)))
        bot-u2g2 (db/with-conn (db/create-bot (:db/id user-2) (:db/id game-2)))
        bot-u2g2 (db/with-conn (db/create-bot (:db/id user-2) (:db/id game-2)))]

   (testing "GET /api/users/:id"
     (let [expected {:id (:db/id user-1)
                     :name (:user/name user-1)
                     :bots [{:name (:bot/name bot-u1g1)
                             :id (:db/id bot-u1g1)
                             :rating (:bot/rating bot-u1g1)
                             :game {:id (:db/id game-1)
                                    :name (:game/name game-1)}}]}]
       (is (= expected (:body (edn-request app :get (str "/api/users/" (:db/id user-1)))))))

     ; TODO test for multiple bots
     )

    (testing "GET /api/games"
      (let [expected [{:id (:db/id game-1)
                       :name (:game/name game-1)
                       :bot-count 1}
                      {:id (:db/id game-2)
                       :name (:game/name game-2)
                       :bot-count 2}
                      ]]
        (is (= (set expected) (set (:body (edn-request app :get "/api/games")))))))

   (testing "GET /api/games/:game-id"
     (let [expected {:id (:db/id game-1)
                     :name (:game/name game-1)
                     :description (:game/description game-1)
                     :bots [{:name (:bot/name bot-u1g1)
                             :rating 1500
                             :id (:db/id bot-u1g1)}]}]
       (is (= expected (:body (edn-request app :get (str "/api/games/" (:db/id game-1))))))

       ; TODO test for multiple bots

       ))


   #_(testing "GET /api/matches/:match-id"
     ; TODO
     (let [expected {:id 890
                     :winner 456
                     :game {:name "foo" :id 123}
                     :bots [{:name "foo" :id 456}]
                     :moves [ {}]}])
     )

   (testing "GET /api/bots/:bot-id"
     (let [expected {:id (:db/id bot-u1g1)
                     :name (:bot/name bot-u1g1)
                     :user {:id (:db/id user-1) :gh-id (:user/gh-id user-1) :name (:user/name user-1)}
                     :game {:id (:db/id game-1) :name (:game/name game-1)}
                     :history (db/with-conn (db/get-bot-history (:db/id bot-u1g1)))
                     :matches (let [matches (db/with-conn (db/get-bot-matches (:db/id bot-u1g1)))]
                                (map (fn [match] {:id (:db/id match)} ) matches)) ; TODO
                     }]
       (is (= expected (:body (edn-request app :get (str "/api/bots/" (:db/id bot-u1g1))))))))

    (testing "POST /api/bots/:bot-id/test"
      (let [goof (db/with-conn (db/create-game "goofspiel" "aaa"))
            user (db/with-conn (db/create-user 1234567 "bloop"))
            bot (db/with-conn (db/create-bot (:db/id user) (:db/id goof)))]
        (db/with-conn (db/update-bot-code (:db/id bot) (pr-str '(fn [state] (get state "current-trophy")))))
        (db/with-conn (db/deploy-bot (:db/id bot)))
        (is true)
        (println (:body (edn-request app :post (str "/api/bots/" (:db/id bot) "/test")))))
      )


    ; TODO following need to be tested with session
    (comment

      (testing "GET /api/bots/:bot-id/code"
        (let [expected {:id (:db/id bot-u1g1)
                        :name (:bot/name bot-u1g1)
                        :user {:id (:db/id user-1) :name (:user/name user-1) :gh-id (:user/gh-id user-1)}
                        :game {:id (:db/id game-1) :name (:game/name game-1)}
                        :code "(fn [] false)"
                        }]
          (is (= expected (:body (edn-request app :get (str "/api/bots/" (:db/id bot-u1g1) "/code")))))))

      (testing "POST /api/games/:game-id/bot"
        (let [expected {:id 999}
              response (edn-request app :post (str "/api/games/" (:db/id game-2) "/bot"))]

          (testing "responds correctly"
            (is (= (keys (:body response)) [:id])))

          (when (:id (:body response))
            (testing "actually creates bot"
              (is (not (nil? (db/get-bot (:id (:body response))))))))))


      (testing "PUT /api/bots/:bot-id/code"
        (let [expected {:status 200}
              code "(fn [] true)"
              response (edn-request app :put (str "/api/bots/" (:db/id bot-u1g1) "/code") {:code code})]

          (testing "responds ok"
            (is (= 200 (:status response))))

          (testing "updates code"
            ; TODO
            )))

      (testing "POST /api/bots/:bot-id/deploy"
        (let [expected {:status 200}
              response (edn-request app :post (str "/api/bots/" (:db/id bot-u1g1) "/deploy"))]
          (testing "responds ok"
            (is (= 200 (:status response))))
          (testing "updates code version"
            ; TODO
            ))))

    )
  )
