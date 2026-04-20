(ns cyberleague.core
  (:gen-class)
  (:require
   [cyberleague.coordinator.core :as coordinator]
   [cyberleague.evaluator.core :as evaluator]
   [cyberleague.server.core :as server]
   [cyberleague.db.seed :as seed]))

(defn start! []
  (evaluator/start!)
  (seed/seed!)
  (server/start!)
  (coordinator/start!))

(defn stop! []
  (coordinator/stop!)
  (server/stop!)
  (evaluator/stop!))

(defn restart! []
  (stop!)
  (start!))

(defn -main []
  (start!))

(comment

  (start!)

  (stop!)

  (restart!)

  )
