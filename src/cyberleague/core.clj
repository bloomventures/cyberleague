(ns cyberleague.core
  (:gen-class)
  (:require
   [bloom.omni.core :as omni]
   [cyberleague.coordinator.core :as coordinator]
   [cyberleague.server.routes :refer [routes]]
   [cyberleague.db.seed :as seed]))

(def config
  {:omni/title "Cyberleague"
   :omni/cljs {:main "cyberleague.client.core"}
   :omni/js-scripts [{:src "/graph.js"}]
   :omni/auth {:cookie {:name "cyberleague"}}
   :omni/api-routes #'routes})

(defn start! []
  (seed/seed!)
  (omni/start! omni/system config)
  #_(coordinator/start!))

(defn stop! []
  (omni/stop!)
  #_(coordinator/stop!))

(defn restart! []
  (stop!)
  (start!))

(defn -main []
  (start!))
