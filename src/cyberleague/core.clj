(ns cyberleague.core
  (:require
   [cyberleague.coordinator.core :as coordinator]
   [cyberleague.server.handler :as handler]
   [cyberleague.server.seed :as seed]))

#_(seed/seed!)
#_(handler/start-server! 5251)
#_(coordinator/start!)
#_(coordinator/stop!)
