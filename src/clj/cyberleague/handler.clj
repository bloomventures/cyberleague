(ns cyberleague.handler
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.tools.nrepl.server :as nrepl]
            [clostache.parser :as clostache]
            [ring.util.response :as response]
            [ring.util.codec :refer  [url-encode]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [org.httpkit.server :refer [run-server]]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [org.httpkit.client :refer [request]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [pog.db :as db]
            [pog.game-runner :as game-runner]))

(def github-redirect-uri (env :github-redirect-uri))
(def github-app-id (env :github-app-id))
(def github-app-secret (env :github-app-secret))
(def datomic-uri (env :datomic-uri))
(def cookie-store-key (env :cookie-store-key))
(def in-prod?
  (= "production" (System/getenv "ENVIRONMENT")))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8" }
   :body (pr-str clj-body)})

(defn string-keys-to-keywords [m]
  (reduce-kv #(assoc %1 (keyword %2) %3) {} m))

(defn to-long [v]
  (java.lang.Long. v))


(defroutes app-routes
  (GET "/" []
    (clostache/render-resource "index.html" (merge {:github-app-id github-app-id
                                                    :github-redirect-uri github-redirect-uri}
                                                   (if in-prod?
                                                     {:production true}
                                                     {:development true}))))

  (GET "/oauth-message" _
    (response/resource-response "oauth-message.html"))

  (GET "/chat/:user-name" [user-name]
    (response/resource-response "hipchat-redirect.html"))

  (POST "/login/:code" [code]
    (let [resp @(request {:url  "https://github.com/login/oauth/access_token"
                          :method :post
                          :headers {"Accept" "application/json"}
                          :query-params {"client_id" github-app-id
                                         "client_secret" github-app-secret
                                         "code" code }
                          } nil)
          token (get (json/read-str (resp :body)) "access_token")
          gh-user (json/read-str (:body @(request {:url  "https://api.github.com/user"
                                                   :method :get
                                                   :headers {"Accept" "application/json"}
                                                   :oauth-token token} nil)))
          user (db/with-conn (db/get-or-create-user (get gh-user "id") (get gh-user "login")))
          out-user {:id (:db/id user)
                    :name (:user/name user)
                    :gh-id (:user/gh-id user)}]

      (assoc (edn-response out-user) :session out-user)))


  (POST "/logout" _
    (assoc (edn-response {:status 200}) :session nil))

  (context "/api" {{:keys [id] :as session} :session}

    (GET "/bots/default/code" [bot-id]
      (edn-response {:id "new"
                     :name (db/gen-bot-name)
                     :code (slurp (io/resource "goofspiel-default.txt"))
                     :user nil
                     :game {:name "goofspiel"}}))

    (GET "/user" _
      (edn-response session))

    (GET "/users" _
      (let [users (db/with-conn (db/get-users))]
        (edn-response (map
                        (fn [user]
                          {:id (:db/id user)
                           :name (:user/name user)
                           :gh-id (:user/gh-id user)
                           ; TODO likely a better way to fetch counts in datomic
                           :bot-count (count (db/with-conn (db/get-user-bots (:db/id user))))}
                          ) users))))

    (GET "/users/:other-user-id" [other-user-id]
      (let [user (db/with-conn (db/get-user (to-long other-user-id)))
            bots (db/with-conn (db/get-user-bots (to-long other-user-id)))]
        (edn-response {:id (:db/id user)
                       :name (:user/name user)
                       :bots (map (fn [bot] {:name (:bot/name bot)
                                             :id (:db/id bot)
                                             :rating (:bot/rating bot)
                                             :status (if (nil? (:bot/code-version bot)) :inactive :active)
                                             :game (let [game (:bot/game bot)]
                                                     {:id (:db/id game)
                                                      :name (:game/name game)})})
                                  bots)})))

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
                                             :status (if (nil? (:bot/code-version bot)) :inactive :active)
                                             :id (:db/id bot) } ) bots)})))

    (GET "/matches/:match-id" [match-id]
      (let [match (db/with-conn (db/get-match (to-long match-id)))]
        (edn-response {:id (:db/id match)
                       :game (let [game (-> match :match/bots first :bot/game)]
                               {:name (:game/name game)
                                :id (:game/id game)})
                       :bots (map (fn [b] {:id (:db/id b)
                                           :name (:bot/name b)})
                                  (:match/bots match))
                       :moves (edn/read-string (:match/moves match))
                       :winner (:db/id (:match/winner match))})))

    (GET "/bots/:bot-id" [bot-id]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))
            matches (db/with-conn (db/get-bot-matches (:db/id bot)))
            history (db/with-conn (db/get-bot-history (:db/id bot)))]
        (edn-response {:id (:db/id bot)
                       :name (:bot/name bot)
                       :game (let [game (:bot/game bot)]
                               {:id (:db/id game)
                                :name (:game/name game) })
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
                                     matches)})))




    (GET "/bots/:bot-id/code" [bot-id]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (= id (:db/id (:bot/user bot)))
          (edn-response {:id (:db/id bot)
                         :name (:bot/name bot)
                         :code (:code/code (:bot/code bot))
                         :language (:code/language (:bot/code bot))
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
        (let [bot (db/with-conn (db/create-bot id (to-long game-id)))]
          (edn-response {:id (:db/id bot)}))
        {:status 500}))

    (PUT "/bots/:bot-id/language/:language" [bot-id language]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (= id (:db/id (:bot/user bot)))
          (let [game (get-in bot [:bot/game :game/name])
                code (slurp (io/resource (str "code/" game "." (case language
                                                                  "javascript" "js"
                                                                  "clojurescript" "cljs"))))
                bot (db/with-conn (db/update-bot-code (to-long bot-id) code language))]
            (edn-response {:code (:code/code (:bot/code bot))
                           :language (:code/language (:bot/code bot))})))))

    (PUT "/bots/:bot-id/code" [bot-id code]
      (if id
        (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
          (if (= id (:db/id (:bot/user bot)))
            (do (db/with-conn (db/update-bot-code (:db/id bot) code (:code/language (:bot/code bot))))
                (edn-response {:status 200}))
            {:status 500}))
        {:status 500}))

    (POST "/bots/:bot-id/test" [bot-id]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (and bot (= id (:db/id (:bot/user bot))))
          ; TODO: use an appropriate random bot for different games
          (let [game-name (get-in bot [:bot/game :game/name])
                random-bot {:db/id (case game-name
                                     "goofspiel" 1234
                                     "ultimate tic-tac-toe" 1235)
                            :bot/code-version 5
                            :bot/deployed-code (slurp (io/resource (str "testbots/" game-name ".cljs")))}
                coded-bot (-> (into {} bot)
                              (assoc :db/id (:db/id bot)
                                :bot/code-version (rand-int 10000000)
                                :bot/deployed-code (get-in bot [:bot/code :code/code])))
                result (game-runner/run-game (into {} (:bot/game bot))
                                             [coded-bot random-bot])
                match {:game {:name game-name}
                       :bots [{:id (:db/id bot) :name "You"} {:id 1234 :name "Them"}]
                       :moves (result :history)
                       :winner (result :winner)
                       :error (result :error)
                       :info (result :info)}]
              (edn-response match))
          {:status 500})))

    (POST "/bots/:bot-id/deploy" [bot-id]
      (let [bot (db/with-conn (db/get-bot (to-long bot-id)))]
        (if (and bot (= id (:db/id (:bot/user bot))))
          (do (db/with-conn (db/deploy-bot (:db/id bot)))
              (edn-response {:status 200}))
          {:status 500})))))

(def app
  (handler/site
    (wrap-edn-params
      (wrap-session
        (routes
          app-routes
          (route/resources "/" ))
        {:store (cookie-store {:key cookie-store-key})}))))

(defonce server (atom nil))

(defn stop-server!
  []
  (when-let [stop-fn @server]
    (stop-fn :timeout 100)))

(defn start-server!
  [port]
  (stop-server!)
  (reset! server (run-server #'app {:port port})))

(defn -main  [& [port nrepl-port & args]]
  (db/init)
  (let [port (if port (Integer/parseInt port) 3000)]
    (start-server! port)
    (println "starting on port " port))
  (try
    (let [nrepl-port (Integer/parseInt nrepl-port)]
      (println "Starting nrepl on port " nrepl-port)
      (nrepl/start-server :port nrepl-port))
    (catch NumberFormatException _
      (println "No nrepl port, not starting"))))

