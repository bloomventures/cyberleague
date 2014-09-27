(ns cyberleague.handler
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [ring.util.codec :refer  [url-encode]]
            [org.httpkit.server :refer  [run-server]]
            [clojure.string :as string]
            [clojure.java.io :as io]))


(defroutes app-routes
  (GET "/" []
    (response/resource-response "index.html")))

(def app (handler/site
           (routes
             app-routes
             (route/resources "/" ))))

(defn -main  [& args]
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-server app {:port port})
    (println "starting on port " port)))

