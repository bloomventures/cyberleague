(ns cyberleague.cli.util.remote
  (:require
   [cyberleague.common.transit-client :as http]
   [cyberleague.cli.util.config-file :as config]
   [cyberleague.cli.util.token :as token]))

(def default-api-url
  "https://cyberleague.rafd.me")

(defn api-url []
  (or (:cyberleague.cli.config/api-server-url (config/read))
      default-api-url))

(defn tada!
  [[event-id params :as opts]]
  (http/request
   {:url (str (api-url)
              "/api/tada/"
              (namespace event-id) "." (name event-id))
    :timeout (:timeout (meta opts))
    :oauth-token (token/read)
    :method :post
    :body {:tada.event/id event-id
           :tada.event/params (or params {})}}))

