(ns cyberleague.server.routes
  (:require
   [cyberleague.db.core :as db]
   [cyberleague.server.oauth :as oauth]
   [cyberleague.server.cqrs :as cqrs]
   [tada.events.ring :as tada.ring]))

(defn wrap-api-token [handler]
  (fn [request]
    (if-let [api-token (some->> (get-in request [:headers "authorization"])
                                (re-matches #"^Bearer ([0-9a-f-]{36})")
                                second
                                parse-uuid)]
      (if-let [user-id (db/token->user-id api-token)]
        (handler (assoc-in request [:session :id] user-id))
        {:status 400
         :body "Invalid API token"})
      (handler request))))

(def routes
  (concat
   oauth/routes
   (->> cqrs/events
        (map (fn [event]
               [(:rest event)
                (fn [request]
                  (db/with-conn
                    (tada.ring/ring-dispatch-event!
                     cqrs/t
                     (:id event)
                     (-> (:params request)
                         (merge (:body-params request))
                         (assoc :user-id (get-in request [:session :id]))))))
                [wrap-api-token]])))
   [[[:post "/api/logout"]
     (fn [_]
       {:session nil})]]))
