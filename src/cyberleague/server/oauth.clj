(ns cyberleague.server.oauth
  (:require
   [clojure.data.json :as json]
   [clojure.string :as string]
   [org.httpkit.client :as http]
   [taoensso.tempel :as tempel]
   [cyberleague.config :refer [config]]
   [cyberleague.db.core :as db])
  (:import
   (java.util Base64)))

;; Oauth requires a state parameter.
;; We will encrypt a value and check if it can be decrypted.
;; The value itself doesn't matter.
;; (The encryption we're using generates a unique message each time,
;; which prevents replay attacks.)

(defn nonce-generate []
  (.encodeToString
   (Base64/getUrlEncoder)
   (tempel/encrypt-with-password
    (byte-array [0])
    (:oauth-nonce-secret config))))

(defn nonce-check [encrypted]
  (try (boolean
        (tempel/decrypt-with-password
         (.decode (Base64/getUrlDecoder)
                  (.getBytes encrypted))
         (:oauth-nonce-secret config)))
       (catch Exception _e
         nil)))

(comment
  (nonce-generate)
  (nonce-check (nonce-generate))
  (nonce-check "random"))

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
      (if (= :dev (config :environment))
        ;; in development, immediately log a user in (first in db)
        {:status 200
         :headers {"Content-Type" "text/html"}
         :session {:id (->> (db/with-conn (db/get-users))
                            first
                            :db/id)}
         :body "<script>window.close();</script>"}
        {:status 302
         :headers {"Location"
                   (str "https://github.com/login/oauth/authorize"
                        "?client_id=" (config :github-client-id)
                        "&redirect_uri=" (config :github-redirect-uri)
                        "&state=" (nonce-generate))}}))]

   [[:get "/oauth/post-auth-redirect"]
    (fn [request]
      (let [{:keys [code state]} (request :params)]
        (if (nonce-check state)
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

