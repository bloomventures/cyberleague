(ns cyberleague.core
  (:gen-class)
  (:require
   [bloom.commons.config :as config]
   [bloom.omni.core :as omni]
   [cyberleague.games.games] ;; so games get registered
   [cyberleague.coordinator.core :as coordinator]
   [cyberleague.server.routes :refer [routes]]
   [cyberleague.db.seed :as seed]))

(def config
  (config/read
   "config.edn"
   [:map
    [:http-port integer?]]))

(def omni-config
  {:omni/http-port (config :http-port)
   :omni/title "Cyberleague"
   :omni/cljs {:main "cyberleague.client.core"}
   :omni/js-scripts [{:src "/graph.js"}]
   :omni/auth {:cookie {:name "cyberleague"}}
   :omni/api-routes #'routes})

(defn start! []
  (seed/seed!)
  (omni/start! omni/system omni-config)
  #_(coordinator/start!))

(defn stop! []
  (omni/stop!)
  #_(coordinator/stop!))

(defn restart! []
  (stop!)
  (start!))

(defn -main []
  (start!))
