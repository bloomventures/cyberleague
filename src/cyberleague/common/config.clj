(ns cyberleague.common.config
  (:require
   [bloom.commons.config :as config]))

(def config
  (config/read
   "config.edn"
   [:map
    [:common
     [:map
      [:evaluator-url :string]
      [:evaluator-auth-secret :string]
      [:environment [:enum :prod :dev]]]]
    [:evaluator
     [:map
      [:http-port :int]
      [:signing-secret :string]]]
    [:server
     [:map
      [:http-port :int]
      [:coordinator-delay :int]
      [:datomic-uri {:optional true}
       [:re #"^datomic:.*"]]
      [:cookie-secret :string]
      [:github-client-id :string] ;; in :dev, can be garbage
      [:github-client-secret :string] ;; in :dev, can be garbage
      [:github-redirect-uri :string] ;; in :dev, can be garbage
      [:oauth-nonce-secret :string]]]]))

