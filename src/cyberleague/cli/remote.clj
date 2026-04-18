(ns cyberleague.cli.remote
  (:require
   [cognitect.transit :as transit]
   [org.httpkit.client :as http]
   [taoensso.telemere :as tel]
   [cyberleague.cli.token :as token])
  (:import
   (java.io ByteArrayOutputStream)))

(def api-root "http://127.0.0.1:3000")

(defn http-request [{:keys [path method body] :as args}]
  (tel/event! ::http-request {:level :debug
                              :data args})
  @(http/request
     {:method method
      :oauth-token (token/read)
      :url (str api-root path)
      :headers {"Accept" "application/transit+json"
                "Content-Type" "application/transit+json"}
      :body (let [out (ByteArrayOutputStream. 4096)
                  writer (transit/writer out :json)]
              (transit/write writer body)
              (.toString out))
      :as :stream}
    (fn [{:keys [status body] :as response}]
      (tel/event! ::http-response {:level :debug
                                   :data response})
      (if (<= 200 status 299)
        (when (pos? (.getCount body))
          (let [b (transit/read (transit/reader body :json))]
            (tel/event! ::http-body {:level :debug
                                     :data b})
            b))
        (println "ERROR: " (slurp body))))))

(defn tada!
  [[event-id params]]
  (http-request {:path (str "/api/tada/"
                            (namespace event-id) "." (name event-id))
                 :method :post
                 :body {:tada.event/id event-id
                        :tada.event/params params}}))
