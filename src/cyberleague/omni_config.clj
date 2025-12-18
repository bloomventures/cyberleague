(ns cyberleague.omni-config
  (:require
    [cyberleague.config :refer [config]]
    [cyberleague.server.routes :refer [routes]]))

(def omni-config
  {:omni/http-port (config :http-port)
   :omni/title "Cyberleague"
   :omni/cljs {:main "cyberleague.client.core"}
   :omni/js-scripts [{:src "/graph.js"}]
   :omni/auth {:cookie {:name "cyberleague"}}
   :omni/api-routes #'routes
   :omni/css {:tailwind? true}})
