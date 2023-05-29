(ns cyberleague.core
  (:gen-class)
  (:require
   [bloom.omni.core :as omni]
   [cyberleague.games.games] ;; so games get registered
   [cyberleague.coordinator.core :as coordinator]
   [cyberleague.db.seed :as seed]
   [cyberleague.omni-config :refer [omni-config]]))

(defn start! []
  (seed/seed!)
  (omni/start! omni/system omni-config)
  (coordinator/start!))

(defn stop! []
  (omni/stop!)
  (coordinator/stop!))

(defn restart! []
  (stop!)
  (start!))

(defn -main []
  (start!))
