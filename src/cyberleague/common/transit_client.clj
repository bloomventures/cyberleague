(ns cyberleague.common.transit-client
  (:require
   [cognitect.transit :as transit]
   [org.httpkit.client :as http]
   [taoensso.telemere :as tel])
  (:import
   (java.io ByteArrayOutputStream)))

(defn file-upload-request
  [{:keys [url method body]}]
  @(http/request
    {:method method
     :url url
     :headers {"Content-Type" "application/octet-stream"}
     :body body}
    (fn [{:keys [status body] :as response}]
      (if (<= 200 status 299)
        nil
        (println "ERROR: " (slurp body))))))

(defn request
  [{:keys [url method body oauth-token] :as args}]
  (tel/event! ::http-request {:level :debug
                              :data args})
  @(http/request
    {:method method
     :url url
     :oauth-token oauth-token
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
