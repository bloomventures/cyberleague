(ns cyberleague.core
  (:require
    [cyberleague.server.handler :as handler]
    [cyberleague.server.seed :as seed]
    [cyberleague.coordinator.core :as coordinator]))

#_(seed/seed!)
#_(handler/start-server! 5251)
#_(coordinator/start!)
#_(coordinator/stop!)
