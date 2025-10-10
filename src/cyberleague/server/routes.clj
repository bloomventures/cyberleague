(ns cyberleague.server.routes
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [cyberleague.db.core :as db]
   [cyberleague.game-registrar :as registrar]
   [cyberleague.coordinator.test-bot :as coordinator]
   [cyberleague.server.oauth :as oauth]
   [cyberleague.schema :as schema] ;; To load custom schema
   [tada.events.core :as tada]
   [tada.events.ring :as tada.ring]))

(defonce t (tada/init :malli))

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

(defn entity-exists?-condition [id-key id]
  [#(db/with-conn (db/entity-exists? id-key id))
   :forbidden
   (str "Entity " id-key " " id " does not exist.")])

(defn user-owns-bot?-condition [user-id bot-id]
  [#(= user-id (:db/id (:bot/user (db/with-conn (db/get-bot bot-id)))))
   :forbidden
   (str "User " user-id " does not own bot " bot-id ".")])

(def events
  [{:id :api/me
    :params {:user-id :user/id}
    :rest [:get "/api/user"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :return (fn [{:keys [user-id]}]
              (let [user (db/with-conn (db/get-user user-id))]
                {:user/id (:db/id user)
                 :user/github-id (:user/github-id user)
                 :user/name (:user/name user)
                 :user/cli-token (:user/cli-token user)}))}
   {:id :api/reset-cli-token!
    :params {:user-id :user/id}
    :rest [:put "/api/cli-token"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :return (fn [{:keys [user-id]}]
              (let [token (db/with-conn (db/reset-cli-token user-id))]
                {:user/cli-token token}))}
   {:id :api/users
    :params {:user-id [:maybe :user/id]}
    :rest [:get "/api/users"]
    :return (fn [{:keys [user-id]}]
              (->> (db/with-conn (db/get-users))
                   (map
                    (fn [user]
                      {:user/id (:db/id user)
                       :user/name (:user/name user)
                       :user/gh-id (:user/gh-id user)
                         ;; TODO: likely a better way to fetch counts in datomic
                       :user/bot-count (count
                                        (db/with-conn
                                          (db/get-user-bots
                                           (:db/id user))))}))))}
   {:id :api/user
    :params {:user-id [:maybe :user/id]
             :other-user-id :user/id}
    :rest [:get "/api/users/:other-user-id"]
    :conditions (fn [{:keys [other-user-id]}]
                  [(entity-exists?-condition :user/id other-user-id)])
    :return (fn [{:keys [other-user-id]}]
              (let [user (db/with-conn (db/get-user other-user-id))
                    bots (db/with-conn (db/get-user-bots other-user-id))]
                {:user/id (:db/id user)
                 :user/name (:user/name user)
                 :user/bots (->> bots
                                 (map
                                  (fn [bot]
                                    {:bot/name (:bot/name bot)
                                     :bot/id (:db/id bot)
                                     :bot/rating (:bot/rating bot)
                                     :bot/status (if
                                                  (nil? (:bot/code-version bot))
                                                   :inactive
                                                   :active)
                                     :bot/game (let [game (:bot/game bot)]
                                                 {:game/id (:db/id game)
                                                  :game/name
                                                  (:game/name game)})})))}))}
   {:id :api/games
    :params {:user-id [:maybe :user/id]}
    :rest [:get "/api/games"]
    :return (fn [_]
              (->> (db/with-conn (db/get-games))
                   (map
                    (fn [game]
                      {:game/id (:db/id game)
                       :game/name (:game/name game)
                         ;; TODO: likely a better way to fetch counts in datomic
                       :game/bot-count (count
                                        (db/with-conn
                                          (db/get-game-bots
                                           (:db/id game))))}))))}

   {:id :api/game
    :params {:user-id [:maybe :user/id]
             :game-id :game/id}
    :rest [:get "/api/games/:game-id"]
    :conditions (fn [{:keys [game-id]}]
                  [(entity-exists?-condition :game/id game-id)])
    :return (fn [{:keys [game-id]}]
              (let [game (db/with-conn (db/get-game game-id))
                    bots (db/with-conn (db/get-game-bots game-id))]
                {:game/id (:db/id game)
                 :game/name (:game/name game)
                 :game/description (:game/description game)
                 :game/bots (->> bots
                                 (map
                                  (fn [bot]
                                    {:bot/user-id (-> bot :bot/user :db/id)
                                     :bot/name (:bot/name bot)
                                     :bot/rating (:bot/rating bot)
                                     :bot/status (if
                                                  (nil? (:bot/code-version bot))
                                                   :inactive
                                                   :active)
                                     :bot/id (:db/id bot)})))}))}
   {:id :api/match
    :params {:user-id [:maybe :user/id]
             :match-id :match/id}
    :rest [:get "/api/matches/:match-id"]
    :conditions (fn [{:keys [match-id]}]
                  [(entity-exists?-condition :match/id match-id)])
    :return (fn [{:keys [match-id]}]
              (let [match (db/with-conn (db/get-match match-id))]
                {:match/id (:db/id match)
                 :match/game (let [game (-> match :match/bots first :bot/game)]
                               {:game/name (:game/name game)
                                :game/id (:game/id game)})
                 :match/bots (map (fn [b]
                                    {:bot/id (:db/id b)
                                     :bot/name (:bot/name b)})
                                  (:match/bots match))
                 :match/moves (edn/read-string (:match/moves match))
                 :match/state-history (edn/read-string
                                       (:match/state-history match))
                 :match/winner (:db/id (:match/winner match))}))}
   {:id :api/bot-id-by-name
    :params {:user-id :user/id
             :bot-name :bot/name}
    :rest [:post "/api/bots/get-id"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :return (fn [{:keys [user-id bot-name]}]
              (when-let [bot-id (db/with-conn (db/get-bot-id user-id bot-name))]
                {:bot-id bot-id}))}
   {:id :api/bot
    :params {:user-id [:maybe :user/id]
             :bot-id :bot/id}
    :rest [:get "/api/bots/:bot-id"]
    :conditions (fn [{:keys [bot-id]}]
                  [(entity-exists?-condition :bot/id bot-id)])
    :return (fn [{:keys [bot-id]}]
              (let [bot (db/with-conn (db/get-bot bot-id))
                    matches (db/with-conn (db/get-bot-matches (:db/id bot)))
                    history (db/with-conn (db/get-bot-history (:db/id bot)))]
                {:bot/id (:db/id bot)
                 :bot/name (:bot/name bot)
                 :bot/game (let [game (:bot/game bot)]
                             {:game/id (:db/id game)
                              :game/name (:game/name game)})
                 :bot/user (let [user (:bot/user bot)]
                             {:user/id (:db/id user)
                              :user/name (:user/name user)
                              :user/gh-id (:user/gh-id user)})
                 :bot/code {:code/language
                            (get-in bot [:bot/code :code/language])}
                 :bot/history history
                 :bot/matches (map
                               (fn [match]
                                 {:match/id (:db/id match)
                                  :match/bots (map (fn [b]
                                                     {:bot/id (:db/id b)
                                                      :bot/name (:bot/name b)})
                                                   (:match/bots match))
                                  :match/winner (:db/id (:match/winner match))})
                               matches)}))}
   {:id :api/bot-code
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:get "/api/bots/:bot-id/code"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)])
    :return (fn [{:keys [user-id bot-id]}]
              (let [bot (db/with-conn (db/get-bot bot-id))]
                {:bot/id (:db/id bot)
                 :bot/name (:bot/name bot)
                 :bot/code {:code/code (:code/code (:bot/code bot))
                            :code/language (:code/language (:bot/code bot))}
                   :bot/user (let [user (:bot/user bot)]
                               {:user/id (:db/id user)
                                :user/name (:user/name user)
                              :user/gh-id (:user/gh-id user)})
                 :bot/game (let [game (:bot/game bot)]
                             {:game/id (:db/id game)
                              :game/name (:game/name game)})}))}
   {:id :api/create-bot!
    :params {:user-id :user/id
             :game-id :game/id}
    :rest [:post "/api/games/:game-id/bot"]
    :conditions (fn [{:keys [user-id game-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :game/id game-id)])
    ;; TODO: Turn :return into an :effect and :return
    :return (fn [{:keys [user-id game-id]}]
              (let [bot (db/with-conn (db/create-bot user-id game-id))]
                {:id (:db/id bot)}))}
   {:id :api/set-bot-language!
    :params {:user-id :user/id
             :bot-id :bot/id
             :language :code/language}
    :rest [:put "/api/bots/:bot-id/language/:language"]
    :conditions (fn [{:keys [user-id bot-id language]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(nil? (:code/language (:bot/code (db/with-conn (db/get-bot bot-id)))))]])
    :return (fn [{:keys [user-id bot-id language]}]
              (let [bot (db/with-conn (db/get-bot bot-id))
                    game-name (get-in bot [:bot/game :game/name])
                    code (get-in @registrar/games [game-name :game.config/starter-code language])
                    bot (db/with-conn (db/update-bot-code bot-id code language))]
                {:bot/code {:code/code (:code/code (:bot/code bot))
                            :code/language (:code/language (:bot/code bot))}}))}
   {:id :api/set-bot-code!
    :params {:user-id :user/id
             :bot-id :bot/id
             :code :code/code}
    :rest [:put "/api/bots/:bot-id/code"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   ;; TODO: Check that bot has a language
                   ])
    :return (fn [{:keys [user-id bot-id code]}]
              (let [bot (db/with-conn (db/get-bot bot-id))]
                (db/with-conn
                  (db/update-bot-code (:db/id bot)
                                      code
                                      (:code/language (:bot/code bot))))))}
   {:id :api/test-bot!
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:post "/api/bots/:bot-id/test"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   ;; TODO: Check that bot has language and code
                   ])
    :return (fn [{:keys [user-id bot-id]}]
              (let [bot (db/with-conn (db/get-bot bot-id))]
                (coordinator/test-bot user-id bot-id bot)))}
   {:id :api/deploy-bot!
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:post "/api/bots/:bot-id/deploy"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   ;; TODO: Check that bot has language, code, and has been tested
                   ])
    :return (fn [{:keys [user-id bot-id]}]
              (let [bot (db/with-conn (db/get-bot bot-id))]
                (db/with-conn (db/deploy-bot (:db/id bot)))))}])

(tada/register! t events)

#_(tada/do! t :api/user {:user-id 5})

(def routes
  (concat
   oauth/routes
   (->> events
        (map (fn [event]
               [(:rest event)
                (fn [request]
                  (tada.ring/ring-dispatch-event!
                   t
                   (:id event)
                   (-> (:params request)
                       (merge (:body-params request))
                       (assoc :user-id (get-in request [:session :id])))))
                [wrap-api-token]])))
   [[[:post "/api/logout"]
     (fn [_]
       {:session nil})]]))
