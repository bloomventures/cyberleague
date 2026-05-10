(ns cyberleague.evaluator.firecracker
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [cyberleague.evaluator.open :as o]
   [org.httpkit.client :as http]
   [taoensso.telemere :as tel])
  (:import
   [java.net StandardProtocolFamily UnixDomainSocketAddress]
   [java.nio.channels SocketChannel]
   [java.lang ProcessBuilder$Redirect]))

(def FirecrackerContext
  [:map
   [:firecracker/socket-path :string]
   [:firecracker/executable-path :string]])

(defn init!
  "Starts a firecracker process. Returns a closeable map; use with with-open+."
  [{:firecracker/keys [executable-path socket-path timeout-seconds] :as context}]
  (tel/event! ::start {:level :info
                       :socket-path socket-path})
  (let [cmd  ["timeout" "--kill-after=1s" (str timeout-seconds "s") executable-path "--api-sock" socket-path "--level" "Error"]
        proc (-> (ProcessBuilder. ^java.util.List cmd)
                 (.redirectOutput ProcessBuilder$Redirect/INHERIT)
                 (.redirectError ProcessBuilder$Redirect/INHERIT)
                 (.start))
        fc   (assoc context ::process proc)]
    (o/with-close-fn
     fc
     (fn [fc]
       (tel/event! ::close {:level :info
                            :socket-path (:firecracker/socket-path fc)})
       (.destroy (::process fc))
       (.waitFor (::process fc))
       (.delete (io/file (:firecracker/socket-path fc)))))))

(defn make-client [socket-path]
  (http/make-client
   {:address-finder
    (fn [_uri]
      (UnixDomainSocketAddress/of socket-path))
    :channel-factory
    (fn [_address]
      (SocketChannel/open StandardProtocolFamily/UNIX))}))

(def memo-client (memoize make-client))

(defn api-request!
  [{socket-path :firecracker/socket-path}
   {:keys [method path body]}]
  (let [opts (cond->
               {:url    (str "http://localhost" path)
                :method method
                :client (memo-client socket-path)}
               body
               (assoc :body (json/generate-string body)
                      :headers {"Content-Type" "application/json"}))
        {:keys [status body error]} @(http/request opts)]
    (when error
      (throw (ex-info (str "Firecracker connection error on " (name method) " " path)
                      {:error error})))
    (let [parsed (json/parse-string body keyword)]
      (when (>= status 400)
        (throw (ex-info (str "Firecracker API error on " (name method) " " path)
                        {:status status
                         :response parsed})))
      parsed)))
