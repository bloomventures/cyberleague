(ns cyberleague.server.oauth
  (:require
    [clojure.string :as string]
    [clojure.data.json :as json]
    [org.httpkit.client :as http]
    [cyberleague.config :refer [config]]
    [cyberleague.db.core :as db]))

(defn get-api-token [{:keys [code state]}]
  (-> @(http/request
         {:method :post
          :url "https://github.com/login/oauth/access_token"
          :headers {"Accept" "application/json"}
          :query-params
          {:client_id (config :github-client-id)
           :client_secret (config :github-client-secret)
           :state state
           :code code}})
      :body
      (json/read-str :key-fn keyword)
      :access_token))

(defn fetch-user-data [api-token]
  (-> @(http/request
         {:method :get
          :url "https://api.github.com/user"
          :headers {"Accept" "application/json"
                    "Authorization" (str "token " api-token)}})
      :body
      (json/read-str :key-fn keyword)
      (select-keys [:login :id])))

(def routes
  [[[:get "/oauth/pre-auth-redirect"]
    (fn [_]
      (let [csrf-token (string/join "" (take 20 (repeatedly #(rand-int 9))))]
        {:status 302
         :headers {"Location"
                   (str "https://github.com/login/oauth/authorize"
                        "?client_id=" (config :github-client-id)
                        "&redirect_uri=" (config :github-redirect-uri)
                        "&state=" csrf-token)}
         :session {:csrf-token csrf-token}}))]

   [[:get "/oauth/post-auth-redirect"]
    (fn [request]
      (let [{:keys [code state]} (request :params)]
        (if (= (get-in request [:session :csrf-token])
               state)
          (if-let [user (-> (get-api-token {:code code
                                            :state state})
                            (fetch-user-data)
                            ((fn [user]
                               (db/with-conn (db/get-or-create-user (user :id) (user :login))))))]
            {:status 200
             :headers {"Content-Type" "text/html"}
             :session {:id (:db/id user)}
             :body "<script>window.close();</script>"}
            {:status 400
             :body "Authentication Failed"})
          {:status 400
           :body "Token was tampered with."})))]])

