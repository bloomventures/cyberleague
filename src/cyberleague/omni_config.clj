(ns cyberleague.omni-config
  (:require
   [cyberleague.common.config :refer [config]]
   [cyberleague.server.routes :refer [routes]]))

(def omni-config
  {:omni/http-port (-> config :server :http-port)
   :omni/environment (-> config :common :environment)
   :omni/title "Cyberleague"
   :omni/cljs {:main "cyberleague.client.core"}
   :omni/js-scripts [{:src "/graph.js"}]
   :omni/html-head-includes
   [[:link {:rel "icon" :type "image/svg+xml" :href "/favicon.min.svg"}]]
   :omni/auth {:cookie {:name "cyberleague"
                        :secret (-> config :server :cookie-secret)}}
   :omni/api-routes #'routes
   :omni/css {:tailwind? true}})
