(ns cyberleague.server.core
  (:require
   [bloom.omni.core :as omni]
   [cyberleague.games.games] ;; so games get registered
   [cyberleague.omni-config :refer [omni-config]]))

(defn start! []
  (omni/start! omni-config))

(defn stop! []
  (omni/stop!))


