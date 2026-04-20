(ns cyberleague.cli.util.remote
  (:require
   [cyberleague.common.transit-client :as http]
   [cyberleague.cli.util.token :as token]))

(def api-root "http://127.0.0.1:3000")

(defn tada!
  [[event-id params]]
  (http/request
   {:url (str api-root
              "/api/tada/"
              (namespace event-id) "." (name event-id))
    :oauth-token (token/read)
    :method :post
    :body {:tada.event/id event-id
           :tada.event/params (or params {})}}))

