(ns cyberleague.server.routes
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [cyberleague.db.core :as db]
   [cyberleague.coordinator.game-runner :as game-runner]))

(defn to-long [v]
  (java.lang.Long. v))

(def routes
  [[[:post "/api/login"]
    (fn [_]
      (let [user (db/with-conn (db/get-or-create-user 38405 "jamesnvc"))
            out-user {:id (:db/id user)
                      :name (:user/name user)
                      :gh-id (:user/gh-id user)}]
        {:body out-user
         :session out-user}))]

   [[:post "/api/logout"]
    (fn [_]
      {:session nil})]

   [[:get "/api/user"]
    (fn [request]
      {:body (request :session)})]

   [[:get "/api/bots/default/code"]
    (fn [_]
      {:body {:id "new"
              :name (db/gen-bot-name)
              :code (slurp (io/resource "goofspiel-default.txt"))
              :user nil
              :game {:name "goofspiel"}}})]

   [[:get "/api/users"]
    (fn [_]
      (let [users (db/with-conn (db/get-users))]
        {:body (->> users
                    (map
                     (fn [user]
                       {:id (:db/id user)
                        :name (:user/name user)
                        :gh-id (:user/gh-id user)
                         ;; TODO likely a better way to fetch counts in datomic
                        :bot-count (count (db/with-conn (db/get-user-bots (:db/id user))))})))}))]

   [[:get "/api/users/:other-user-id"]
    (fn [request]
      (let [other-user-id (get-in request [:params :other-user-id])
            user (db/with-conn (db/get-user (to-long other-user-id)))
            bots (db/with-conn (db/get-user-bots (to-long other-user-id)))]
        {:body {:id (:db/id user)
                :name (:user/name user)
                :bots (->> bots
                           (map (fn [bot]
                                  {:name (:bot/name bot)
                                   :id (:db/id bot)
                                   :rating (:bot/rating bot)
                                   :status (if (nil? (:bot/code-version bot)) :inactive :active)
                                   :game (let [game (:bot/game bot)]
                                           {:id (:db/id game)
                                            :name (:game/name game)})})))}}))]

   [[:get "/api/games"]
    (fn [_]
      {:body (->> (db/with-conn (db/get-games))
                  (map
                   (fn [game]
                     {:id (:db/id game)
                      :name (:game/name game)
                       ;; TODO likely a better way to fetch counts in datomic
                      :bot-count (count (db/with-conn (db/get-game-bots (:db/id game))))})))})]

   [[:get "/api/games/:game-id"]
    (fn [request]
      (let [game-id (get-in request [:params :game-id])
            game (db/with-conn (db/get-game (to-long game-id)))
            bots (db/with-conn (db/get-game-bots (to-long game-id)))]
        {:body {:id (:db/id game)
                :name (:game/name game)
                :description (:game/description game)
                :bots (->> bots
                           (map (fn [bot]
                                  {:name (:bot/name bot)
                                   :rating (:bot/rating bot)
                                   :status (if (nil? (:bot/code-version bot)) :inactive :active)
                                   :id (:db/id bot)})))}}))]

   [[:get "/api/matches/:match-id"]
    (fn [request]
      (let [match-id (get-in request [:params :match-id])
            match (db/with-conn (db/get-match (to-long match-id)))]
        {:body {:id (:db/id match)
                :game (let [game (-> match :match/bots first :bot/game)]
                        {:name (:game/name game)
                         :id (:game/id game)})
                :bots (map (fn [b] {:id (:db/id b)
                                    :name (:bot/name b)})
                           (:match/bots match))
                :moves (edn/read-string (:match/moves match))
                :winner (:db/id (:match/winner match))}}))]

   [[:get "/api/bots/:bot-id"]
    (fn [request]
      (let [bot-id (get-in request [:params :bot-id])
            bot (db/with-conn (db/get-bot (to-long bot-id)))
            matches (db/with-conn (db/get-bot-matches (:db/id bot)))
            history (db/with-conn (db/get-bot-history (:db/id bot)))]
        {:body {:id (:db/id bot)
                :name (:bot/name bot)
                :game (let [game (:bot/game bot)]
                        {:id (:db/id game)
                         :name (:game/name game)})
                :user (let [user (:bot/user bot)]
                        {:id (:db/id user)
                         :name (:user/name user)
                         :gh-id (:user/gh-id user)})
                :history history
                :matches (map (fn [match]
                                {:id (:db/id match)
                                 :bots (map (fn [b] {:id (:db/id b)
                                                     :name (:bot/name b)})
                                            (:match/bots match))
                                 :winner (:db/id (:match/winner match))})
                              matches)}}))]

   [[:get "/api/bots/:bot-id/code"]
    (fn [request]
      (let [user-id (get-in request [:session :id])
            bot-id (get-in request [:params :bot-id])
            bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (= user-id (:db/id (:bot/user bot)))
          {:body {:id (:db/id bot)
                  :name (:bot/name bot)
                  :code (:code/code (:bot/code bot))
                  :language (:code/language (:bot/code bot))
                  :user (let [user (:bot/user bot)]
                          {:id (:db/id user)
                           :name (:user/name user)
                           :gh-id (:user/gh-id user)})
                  :game (let [game (:bot/game bot)]
                          {:id (:db/id game)
                           :name (:game/name game)})}}
          {:status 500})))]

   [[:post "/api/games/:game-id/bot"]
    (fn [request]
      (if-let [user-id (get-in request [:session :id])]
        (let [game-id (get-in request [:params :game-id])
              bot (db/with-conn (db/create-bot user-id (to-long game-id)))]
          {:body {:id (:db/id bot)}})
        {:status 500}))]

   [[:put "/api/bots/:bot-id/language/:language"]
    (fn [request]
      (let [user-id (get-in request [:session :id])
            bot-id (get-in request [:params :bot-id])
            language (get-in request [:params :language])
            bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (= user-id (:db/id (:bot/user bot)))
          (let [game (get-in bot [:bot/game :game/name])
                code (slurp (io/resource (str "code/" game "." (case language
                                                                 "javascript" "js"
                                                                 "clojure" "cljs"))))
                bot (db/with-conn (db/update-bot-code (to-long bot-id) code language))]
            {:body {:code (:code/code (:bot/code bot))
                    :language (:code/language (:bot/code bot))}}))))]

   [[:put "/api/bots/:bot-id/code"]
    (fn [request]
      (if-let [user-id (get-in request [:session :id])]
        (let [bot-id (get-in request [:params :bot-id])
              bot (db/with-conn (db/get-bot (to-long bot-id)))
              code (get-in request [:body-params :code])]
          (if (= user-id (:db/id (:bot/user bot)))
            (do
              (db/with-conn (db/update-bot-code (:db/id bot) code (:code/language (:bot/code bot))))
              {:status 200})
            {:status 500}))
        {:status 500}))]

   [[:post "/api/bots/:bot-id/test"]
    (fn [request]
      (let [user-id (get-in request [:session :id])
            bot-id (get-in request [:params :bot-id])
            bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (and bot (= user-id (:db/id (:bot/user bot))))
          ; TODO: use an appropriate random bot for different games
          (let [game-name (get-in bot [:bot/game :game/name])
                random-bot {:db/id (case game-name
                                     "goofspiel" 1234
                                     "ultimate tic-tac-toe" 1235)
                            :bot/code-version 5
                            :bot/code {:code/code (slurp (io/resource (str "testbots/" game-name ".cljs")))
                                       :code/language "clojure"}}
                coded-bot (-> (into {} bot)
                              (assoc :db/id (:db/id bot)
                                     :bot/code-version (rand-int 10000000)
                                     :bot/code (:bot/code bot)))
                result (game-runner/run-game (into {} (:bot/game bot))
                                             [coded-bot random-bot])
                match {:game {:name game-name}
                       :bots [{:id (:db/id bot) :name "You"} {:id 1234 :name "Them"}]
                       :moves (result :history)
                       :winner (result :winner)
                       :error (result :error)
                       :info (result :info)}]
            {:body match})
          {:status 500})))]

   [[:post "/api/bots/:bot-id/deploy"]
    (fn [request]
      (let [user-id (get-in request [:session :id])
            bot-id (get-in request [:params :bot-id])
            bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (and bot (= user-id (:db/id (:bot/user bot))))
          (do (db/with-conn (db/deploy-bot (:db/id bot)))
              {:status 200})
          {:status 500})))]])
