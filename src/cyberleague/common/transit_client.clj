(ns cyberleague.common.transit-client
  (:require
   [org.httpkit.client :as http]
   [taoensso.telemere :as tel]
   [cyberleague.common.transit :as t]))

(defn file-upload-request
  [{:keys [url method body]}]
  @(http/request
    {:method method
     :url url
     :headers {"Content-Type" "application/octet-stream"}
     :body body}
    (fn [{:keys [status body] :as response}]
      (if (and status (<= 200 status 299))
        nil
        (throw (ex-info "Error Uploading" {:body (slurp body)}) )))))

(defn request
  [{:keys [url method body oauth-token timeout] :as args}]
  (tel/event! ::http-request {:level :debug
                              :data args})
  @(http/request
    {:method method
     :url url
     :timeout (or timeout 5000)
     :oauth-token oauth-token
     :headers {"Accept" "application/transit+json"
               "Content-Type" "application/transit+json"}
     :body (t/write-str body)
     :as :stream}
    (fn [{:keys [status body] :as response}]
      (tel/event! ::http-response {:level :debug
                                   :data response})
      (if (and status (<= 200 status 299))
        (when (pos? (.getCount ^org.httpkit.BytesInputStream body))
          (let [b (t/read body)]
            (tel/event! ::http-body {:level :debug
                                     :data b})
            b))
        (tel/event! ::http-error {:level :error
                                  :data {:body (slurp body)}})))))
