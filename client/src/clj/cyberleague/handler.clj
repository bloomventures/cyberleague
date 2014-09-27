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
    (response/resource-response "index.html"))


  (context "/api" _

    (GET "/games" _)

    (GET "/games/:id" [id])

    (GET "/games/:id/rules" [id])

    (GET "/games/:id/code" [id])

    (GET "/bots/:id" [id])

    (PUT "/bots/:id" [id])

    (POST "/bots/:id/deploy" [id])

    (GET "/matches/:id" [id])


    ))

(def app (handler/site
           (routes
             app-routes
             (route/resources "/" ))))

(defn -main  [& args]
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-server app {:port port})
    (println "starting on port " port)))

