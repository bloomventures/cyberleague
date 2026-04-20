(ns cyberleague.server.evaluator-client
  (:require
   [cyberleague.common.transit-client :as http]
   [cyberleague.common.config :as config]))

(defn prepare
  [{:keys [digest]}]
  (http/request
   {:method :post
    :url (str (-> config/config
                  :common
                  :evaluator-url)
              "/prepare")
    :body {:digest digest}}))

#_(prepare {:digest "a6213fe8321a5ce0e65e3560ba0eda5d169352dbd2e02e653b10d109eeff56d0"})

(defn eval!
  [{:keys [digest env-slug input]}]
  (http/request
   {:method :post
    :url (str (-> config/config
                  :common
                  :evaluator-url)
              "/run")
    :body {:digest digest
           :env-slug env-slug
           :input input}}))

