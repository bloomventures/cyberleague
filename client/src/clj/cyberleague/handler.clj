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

(defn edn-response [clj-body]
  {:headers {"Content-Type" "application/edn; charset=utf-8" }
   :body (pr-str clj-body)})

(defroutes app-routes
  (GET "/" []
    (response/resource-response "index.html"))


  (context "/api" _

    (GET "/games" _
      (edn-response [{:id 123 :name "foo" :bot-count 123}]))

    (GET "/games/:id" [id]
      (edn-response {:id 123
                     :name "foo"
                     :description "foo description"
                     :bots [{:name "mk36"
                             :rating 100
                             :id 456 }]}))

    (GET "/games/:id/rules" [id]
      (edn-response {:id 123
                     :name "foo"
                     :rules "foo rules"}))

    (GET "/matches/:id" [id]
      (edn-response {:id 890
                     :winner 456
                     :game {:name "foo" :id 123}
                     :bots [{:name "foo" :id 456}]
                     :moves [ {} ]}))

    (GET "/bots/:id" [id]
      (edn-response {:id 123
                     :name "foo"
                     :user {:id 555 :name "person"}
                     :game {:id 123 :name "foo"}
                     :history [{:rating 123 :rating-dev 123 :code-version 999}]
                     :matches [{:id 888 :winner 456 :bots [{:name "foo" :id 456}]}]}))

    (GET "/bots/:id/code" [id]
      (edn-response {:id 123
                     :name "foo"
                     :user {:id 555 :name "person"}
                     :game {:id 123 :name "foo"}
                     :code "(fn [state])"}))

    (PUT "/bots/:id" [id]
      (edn-response {:status "OK"}))

    (POST "/bots/:id/deploy" [id]
      (edn-response {:status "OK"}))))

(def app (handler/site
           (routes
             app-routes
             (route/resources "/" ))))

(defn -main  [& [port & args]]
  (let [port (if port (Integer/parseInt port) 3000)]
    (run-server app {:port port})
    (println "starting on port " port)))

