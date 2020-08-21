(ns cyberleague.config
  (:require
    [bloom.commons.config :as config]))

(def config
  (config/read
   "config.edn"
   [:map
    [:http-port integer?]
    [:environment [:enum :prod :dev]]
    [:github-client-id string?] ;; in :dev, can be garbage
    [:github-client-secret string?] ;; in :dev, can be garbage
    [:github-redirect-uri string?] ;; in :dev, can be garbage
    ]))
