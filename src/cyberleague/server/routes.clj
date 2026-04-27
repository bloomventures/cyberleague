(ns cyberleague.server.routes
  (:require
   [clojure.walk :as walk]
   [tada.events.ring :as tada.ring]
   [cyberleague.db.core :as db]
   [cyberleague.server.cqrs :as cqrs]
   [cyberleague.server.oauth :as oauth]))

(defn wrap-api-token [handler]
  (fn [request]
    (if-let [api-token (some->> (get-in request [:headers "authorization"])
                                (re-matches #"^Bearer ([0-9a-f-]{36})")
                                second
                                parse-uuid)]
      (if-let [user-id (db/with-conn (db/token->user-id api-token))]
        (handler (assoc-in request [:session :id] user-id))
        {:status 400
         :body "Invalid API token; run login"})
      (handler request))))

(defn make-tada-handler
  [request->tada-params]
  (fn [request]
    (let [{:tada.event/keys [id params]} (request->tada-params request)]
      (if (and (keyword? id)
               (map? params))
        (db/with-conn
         (-> (tada.ring/ring-dispatch-event!
              cqrs/t
              id
              (-> params
                  (assoc :user-id (get-in request [:session :id]))))
             ; Need to consume lazy sequences before we leave the db/with-conn
             (update :body (fn [v] (walk/postwalk identity v)))
             ((fn [response] (if (string? (:body response))
                               (assoc-in response [:headers "Content-Type"] "text/plain")
                               response)))))
        (throw (ex-info "Incorrect TADA params" {}))))))

(def routes
  (concat
   oauth/routes

   ;; RESTful tada handlers
   (->> cqrs/events
        (map (fn [event]
               [(:rest event)
                (make-tada-handler
                 (fn [request]
                   {:tada.event/id (:id event)
                    :tada.event/params (-> (:params request)
                                           (merge (:body-params request)))}))
                [wrap-api-token]])))
   [
    ;; generic tada handler
    [[:post "/api/tada/*"]
     ;; expects body to have {:event-id _ :event-params _}
     (make-tada-handler :body-params)
     [wrap-api-token]]

    [[:post "/api/logout"]
     (fn [_]
       {:session nil})]]))
