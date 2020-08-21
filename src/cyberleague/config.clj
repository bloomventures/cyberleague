(ns cyberleague.config
  (:require
    [bloom.commons.config :as config]))

(def config
  (config/read
   "config.edn"
   [:map
    [:http-port integer?]
    [:github-client-id string?]
    [:github-client-secret string?]
    [:github-redirect-uri string?]]))
