(ns cyberleague.core
  (:gen-class)
  (:require
   [cyberleague.coordinator.core :as coordinator]
   [cyberleague.server.core :as server]))

(defn start! []
  (server/start!)
  (coordinator/start!))

(defn stop! []
  (coordinator/stop!)
  (server/stop!))

(defn -main []
  (start!))


