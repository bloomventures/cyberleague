(ns cyberleague.core
  (:gen-class)
  (:require
   [taoensso.telemere :as tel]
   [cyberleague.coordinator.core :as coordinator]
   [cyberleague.server.core :as server]))

(tel/uncaught->error!)

(defn start! []
  (server/start!)
  (coordinator/start!))

(defn stop! []
  (coordinator/stop!)
  (server/stop!))

(defn -main []
  (start!))


