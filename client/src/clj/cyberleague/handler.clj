(ns cyberleague.handler
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [ring.util.codec :refer  [url-encode]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [org.httpkit.server :refer  [run-server]]
            [clojure.data.json :as json]
            [org.httpkit.client :refer [request]]
            [clojure.string :as string]
            [clojure.java.io :as io]))

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8" }
   :body (pr-str clj-body)})

(defn string-keys-to-keywords [m]
  (reduce-kv #(assoc %1 (keyword %2) %3) {} m))

(defroutes app-routes
  (GET "/" []
    (response/resource-response "index.html"))

  (GET "/oauth-message" _
    (response/resource-response "oauth-message.html"))

  (POST "/login/:code" [code]
    (let [resp @(request {:url  "https://github.com/login/oauth/access_token"
                          :method :post
                          :headers {"Accept" "application/json"}
                          :query-params {"client_id" "***REMOVED***"
                                         "client_secret" "***REMOVED***"
                                         "code" code }
                          } nil)
          token (get (json/read-str (resp :body)) "access_token")
          user-raw (json/read-str (:body @(request {:url  "https://api.github.com/user"
                                                    :method :get
                                                    :headers {"Accept" "application/json"}
                                                    :oauth-token token} nil)))
          user {:id (get user-raw "id")
                :avatar_url (get user-raw "avatar_url")
                :name (get user-raw "login")}]

      (assoc (edn-response user) :session user)))

   (POST "/logout" _
     (assoc (edn-response {:status "OK"}) :session nil))

  (context "/api" {session :session}

    (GET "/user" _
         (edn-response session))

    (GET "/games" _
      (edn-response [{:id 123 :name "foo" :bot-count 123}]))

    (GET "/games/:id" [id]
      (edn-response {:id 123
                     :name "foo"
                     :description "foo description"
                     :bots [{:name "mk36"
                             :rating 100
                             :id 456 }]}))

    (GET "/games/:id/rules" [id]
      (edn-response {:id 123
                     :name "foo"
                     :rules "foo rules"}))

    (GET "/matches/:id" [id]
      (edn-response {:id 890
                     :winner 456
                     :game {:name "foo" :id 123}
                     :bots [{:name "foo" :id 456}]
                     :moves [ {} ]}))

    (GET "/bots/:id" [id]
      (edn-response {:id 123
                     :name "foo"
                     :user {:id 555 :name "person"}
                     :game {:id 123 :name "foo"}
                     :history [{:rating 123 :rating-dev 123 :code-version 999}]
                     :matches [{:id 888 :winner 456 :bots [{:name "foo" :id 456}]}]}))

    (GET "/bots/:id/code" [id]
      (edn-response {:id 123
                     :name "foo"
                     :user {:id 555 :name "person"}
                     :game {:id 123 :name "foo"}
                     :code "(fn [state])"}))

    (PUT "/bots/:id" [id]
      (edn-response {:status "OK"}))

    (POST "/bots/:id/deploy" [id]
      (edn-response {:status "OK"}))))

(def app (handler/site
           (wrap-session
             (routes
               app-routes
               (route/resources "/" ))
             {:store (cookie-store {:key "***REMOVED***"})})))

(defn -main  [& [port & args]]
  (let [port (if port (Integer/parseInt port) 3000)]
    (run-server app {:port port})
    (println "starting on port " port)))

