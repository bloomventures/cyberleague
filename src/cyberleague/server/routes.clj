(ns cyberleague.server.routes
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [cyberleague.db.core :as db]
   [cyberleague.game-registrar :as registrar]
   [cyberleague.coordinator.test-bot :as coordinator]
   [cyberleague.server.oauth :as oauth]))

(defn to-long [v]
  (java.lang.Long. v))

(defn wrap-api-token [handler]
  (fn [request]
    (if-let [api-token (some->> (get-in request [:headers "authorization"])
                                (re-matches #"^Bearer ([0-9a-f-]{36})")
                                second
                                parse-uuid)]
      (if-let [user-id (db/with-conn (db/token->user-id api-token))]
        (handler (assoc-in request [:session :id] user-id))
        {:status 400
         :body "Invalid API token"})
      (handler request))))

(def routes
  (concat
    oauth/routes
    [[[:get "/api/user"]
      (fn [request]
        {:body (when-let [user (db/with-conn (db/get-user (get-in request [:session :id])))]
                 {:user/id (:db/id user)
                  :user/github-id (:user/github-id user)
                  :user/name (:user/name user)
                  :user/cli-token (:user/cli-token user)})})]

     [[:put "/api/cli-token"]
      (fn [request]
        {:body (when-let [token (db/with-conn (db/reset-cli-token
                                              (get-in request [:session :id])))]
                 {:user/cli-token token})})]

     [[:post "/api/logout"]
      (fn [_]
        {:session nil})]

     [[:get "/api/users"]
      (fn [_]
        (let [users (db/with-conn (db/get-users))]
          {:body (->> users
                      (map
                        (fn [user]
                          {:user/id (:db/id user)
                           :user/name (:user/name user)
                           :user/gh-id (:user/gh-id user)
                           ;; TODO likely a better way to fetch counts in datomic
                           :user/bot-count (count (db/with-conn (db/get-user-bots (:db/id user))))})))}))]

     [[:get "/api/users/:other-user-id"]
      (fn [request]
        (let [other-user-id (get-in request [:params :other-user-id])
              user (db/with-conn (db/get-user (to-long other-user-id)))
              bots (db/with-conn (db/get-user-bots (to-long other-user-id)))]
          {:body {:user/id (:db/id user)
                  :user/name (:user/name user)
                  :user/bots (->> bots
                                  (map (fn [bot]
                                         {:bot/name (:bot/name bot)
                                          :bot/id (:db/id bot)
                                          :bot/rating (:bot/rating bot)
                                          :bot/status (if (nil? (:bot/code-version bot)) :inactive :active)
                                          :bot/game (let [game (:bot/game bot)]
                                                      {:game/id (:db/id game)
                                                       :game/name (:game/name game)})})))}}))]

     [[:get "/api/games"]
      (fn [_]
        {:body (->> (db/with-conn (db/get-games))
                    (map
                      (fn [game]
                        {:game/id (:db/id game)
                         :game/name (:game/name game)
                         ;; TODO likely a better way to fetch counts in datomic
                         :game/bot-count (count (db/with-conn (db/get-game-bots (:db/id game))))})))})]

     [[:get "/api/games/:game-id"]
      (fn [request]
        (let [game-id (get-in request [:params :game-id])
              game (db/with-conn (db/get-game (to-long game-id)))
              bots (db/with-conn (db/get-game-bots (to-long game-id)))]
          {:body {:game/id (:db/id game)
                  :game/name (:game/name game)
                  :game/description (:game/description game)
                  :game/bots (->> bots
                                  (map (fn [bot]
                                         {:bot/user-id (-> bot :bot/user :db/id)
                                          :bot/name (:bot/name bot)
                                          :bot/rating (:bot/rating bot)
                                          :bot/status (if (nil? (:bot/code-version bot)) :inactive :active)
                                          :bot/id (:db/id bot)})))}}))]

     [[:get "/api/matches/:match-id"]
      (fn [request]
        (let [match-id (get-in request [:params :match-id])
              match (db/with-conn (db/get-match (to-long match-id)))]
          {:body {:match/id (:db/id match)
                  :match/game (let [game (-> match :match/bots first :bot/game)]
                                {:game/name (:game/name game)
                                 :game/id (:game/id game)})
                  :match/bots (map (fn [b]
                                     {:bot/id (:db/id b)
                                      :bot/name (:bot/name b)})
                                   (:match/bots match))
                  :match/moves (edn/read-string (:match/moves match))
                  :match/state-history (edn/read-string (:match/state-history match))
                  :match/winner (:db/id (:match/winner match))}}))]

     [[:get "/api/bots/:bot-id"]
      (fn [request]
        (let [bot-id (get-in request [:params :bot-id])
              bot (db/with-conn (db/get-bot (to-long bot-id)))
              matches (db/with-conn (db/get-bot-matches (:db/id bot)))
              history (db/with-conn (db/get-bot-history (:db/id bot)))]
          {:body {:bot/id (:db/id bot)
                  :bot/name (:bot/name bot)
                  :bot/game (let [game (:bot/game bot)]
                              {:game/id (:db/id game)
                               :game/name (:game/name game)})
                  :bot/user (let [user (:bot/user bot)]
                              {:user/id (:db/id user)
                               :user/name (:user/name user)
                               :user/gh-id (:user/gh-id user)})
                  :bot/code {:code/language (get-in bot [:bot/code :code/language])}
                  :bot/history history
                  :bot/matches (map (fn [match]
                                      {:match/id (:db/id match)
                                       :match/bots (map (fn [b]
                                                           {:bot/id (:db/id b)
                                                             :bot/name (:bot/name b)})
                                                        (:match/bots match))
                                       :match/winner (:db/id (:match/winner match))})
                                    matches)}}))]

     [[:get "/api/bots/:bot-id/code"]
      (fn [request]
        (let [user-id (get-in request [:session :id])
              bot-id (get-in request [:params :bot-id])
              bot (db/with-conn (db/get-bot (to-long bot-id)))]
          (if (= user-id (:db/id (:bot/user bot)))
            {:body {:bot/id (:db/id bot)
                    :bot/name (:bot/name bot)
                    :bot/code {:code/code (:code/code (:bot/code bot))
                               :code/language (:code/language (:bot/code bot))}
                    :bot/user (let [user (:bot/user bot)]
                                {:user/id (:db/id user)
                                 :user/name (:user/name user)
                                 :user/gh-id (:user/gh-id user)})
                    :bot/game (let [game (:bot/game bot)]
                                {:game/id (:db/id game)
                                 :game/name (:game/name game)})}}
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
                  code (get-in @registrar/games [game :game.config/starter-code language])
                  bot (db/with-conn (db/update-bot-code (to-long bot-id) code language))]
              {:body {:bot/code {:code/code (:code/code (:bot/code bot))
                                 :code/language (:code/language (:bot/code bot))}}})
            {:status 500})))]

     [[:post "/api/bots/get-id"]
      (fn [request]
        (if-let [user-id (get-in request [:session :id])]
          (let [bot-name (get-in request [:body-params :bot/name])
                bot-id (db/with-conn (db/get-bot-id user-id bot-name))]
            (if bot-id
              {:status 200
               :body {:bot-id bot-id}}
              {:status 500}))
          {:status 500}))
      [wrap-api-token]]

     [[:put "/api/bots/:bot-id/code"]
      (fn [request]
        (if-let [user-id (get-in request [:session :id])]
          (let [bot-id (get-in request [:params :bot-id])
                bot (db/with-conn (db/get-bot (to-long bot-id)))
                code (get-in request [:body-params :bot/code])]
            (if (= user-id (:db/id (:bot/user bot)))
              (do
                (db/with-conn (db/update-bot-code (:db/id bot) code (:code/language (:bot/code bot))))
                {:status 200})
              {:status 500}))
          {:status 500}))
      [wrap-api-token]]

     [[:post "/api/bots/:bot-id/test"]
      (fn [request]
        (let [user-id (get-in request [:session :id])
              bot-id (get-in request [:params :bot-id])
              bot (db/with-conn (db/get-bot (to-long bot-id)))]
          (if (and bot (= user-id (:db/id (:bot/user bot))))
            {:body (coordinator/test-bot user-id bot-id bot)}
            {:status 500})))]

     [[:post "/api/bots/:bot-id/deploy"]
      (fn [request]
        (let [user-id (get-in request [:session :id])
              bot-id (get-in request [:params :bot-id])
              bot (db/with-conn (db/get-bot (to-long bot-id)))]
          (if (and bot (= user-id (:db/id (:bot/user bot))))
            (do (db/with-conn (db/deploy-bot (:db/id bot)))
                {:status 200})
            {:status 500})))]]))
