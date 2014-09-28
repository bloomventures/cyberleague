(ns cyberleague.handler
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [ring.util.codec :refer  [url-encode]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [org.httpkit.server :refer [run-server]]
            [clojure.data.json :as json]
            [org.httpkit.client :refer [request]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [cyberleague.db :as db]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8" }
   :body (pr-str clj-body)})

(defn string-keys-to-keywords [m]
  (reduce-kv #(assoc %1 (keyword %2) %3) {} m))

(defn to-long [v]
  (java.lang.Long. v))

(defroutes app-routes
  (GET "/" []
    (response/resource-response "index.html"))

  (GET "/oauth-message" _
    (response/resource-response "oauth-message.html"))

  (POST "/login/:code" [code]
    (let [resp @(request {:url  "https://github.com/login/oauth/access_token"
                          :method :post
                          :headers {"Accept" "application/json"}
                          :query-params {"client_id" "c3e1d987d59e4ab7f433"
                                         "client_secret" "62ace985f21627c0d4c6703ac24fed3b38cb3447"
                                         "code" code }
                          } nil)
          token (get (json/read-str (resp :body)) "access_token")
          gh-user (json/read-str (:body @(request {:url  "https://api.github.com/user"
                                                   :method :get
                                                   :headers {"Accept" "application/json"}
                                                   :oauth-token token} nil)))
          user (db/with-conn (db/create-user (get gh-user "id") (get gh-user "login")))
          out-user {:id (:db/id user)
                    :name (:user/name user)
                    :gh-id (:user/gh-id user)}]

      (assoc (edn-response out-user) :session out-user)))


  (POST "/logout" _
    (assoc (edn-response {:status 200}) :session nil))

  (context "/api" {{:keys [id] :as session} :session}

    (GET "/user" _
      (edn-response session))

    (GET "/users/:other-user-id" [other-user-id]
      (let [user (db/with-conn (db/get-user (to-long other-user-id)))
            bots (db/with-conn (db/get-user-bots (to-long other-user-id)))]
        (edn-response {:id (:db/id user)
                       :name (:user/name user)
                       :bots (map (fn [bot] {:name (:bot/name bot)
                                             :id (:db/id bot)
                                             :rating (:bot/rating bot)
                                             :game (let [game (:bot/game bot)]
                                                     {:id (:db/id game)
                                                      :name (:game/name game)})} ) bots)})))

    (GET "/games" _
      (let [games (db/with-conn (db/get-games))]
        (edn-response (map
                        (fn [game]
                          {:id (:db/id game)
                           :name (:game/name game)
                           ; TODO likely a better way to fetch counts in datomic
                           :bot-count (count (db/with-conn (db/get-game-bots (:db/id game))))}
                          ) games))))

    (GET "/games/:game-id" [game-id]
      (let [game (db/with-conn (db/get-game (to-long game-id)))
            bots (db/with-conn (db/get-game-bots (to-long game-id)))]
        (edn-response {:id (:db/id game)
                       :name (:game/name game  )
                       :description (:game/description game)
                       :bots (map (fn [bot] {:name (:bot/name bot)
                                             :rating (:bot/rating bot)
                                             :id (:db/id bot) } ) bots)})))

    (GET "/matches/:match-id" [match-id]
      (let [match (db/with-conn (db/get-match (to-long match-id)))]
        (edn-response {:id (:db/id match)
                       ; TODO
                       })))

    (GET "/bots/:bot-id" [bot-id]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (edn-response {:id (:db/id bot)
                       :name (:bot/name bot)
                       :game (let [game (:bot/game bot)]
                               {:id (:db/id game)
                                :name (:game/name game) })
                       :user (let [user (:bot/user bot)]
                               {:id (:db/id user)
                                :name (:user/name user)
                                :gh-id (:user/gh-id user)})
                       ;                       :history nil ; TODO
                       ;                       :matches nil ; TODO
                       })))

    (GET "/bots/:bot-id/code" [bot-id]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (= id (:db/id (:bot/user bot)))
          (edn-response {:id (:db/id bot)
                         :name (:bot/name bot)
                         :code (:code/code (:bot/code bot))
                         :user (let [user (:bot/user bot)]
                                 {:id (:db/id user)
                                  :name (:user/name user)
                                  :gh-id (:user/gh-id user)})
                         :game (let [game (:bot/game bot)]
                                 {:id (:db/id game)
                                  :name (:game/name game) }) })
          {:status 500})))

    (POST "/games/:game-id/bot" [game-id]
      (if id
        (let [bot (db/with-conn (db/create-bot id (to-long game-id)))
              _ (db/with-conn (db/update-bot-code (:db/id bot) "(fn [state] )"))]
          (edn-response {:id (:db/id bot)}))
        {:status 500}))

    (PUT "/bots/:bot-id/code" [bot-id code]
      (if id
        (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
          (if (= id (:db/id (:bot/user bot)))
            (do (db/with-conn (db/update-bot-code (:db/id bot) code))
                (edn-response {:status 200}))
            {:status 500}))
        {:status 500}))

    (POST "/bots/:bot-id/deploy" [bot-id]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (and bot (= id (:db/id (:bot/user bot))))
          (do (db/with-conn (db/deploy-bot (:db/id bot)))
              (edn-response {:status 200}))
          {:status 500})))))

(def app (handler/site
           (wrap-edn-params
             (wrap-session
               (routes
                 app-routes
                 (route/resources "/" ))
               {:store (cookie-store {:key "runG4aurf8ek9caK"})}))))

(defn -main  [& [port & args]]
  (db/init)
  (let [port (if port (Integer/parseInt port) 3000)]
    (run-server app {:port port})
    (println "starting on port " port)))

