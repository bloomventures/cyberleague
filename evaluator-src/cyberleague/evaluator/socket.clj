(ns cyberleague.evaluator.socket
  (:require
   [clj-cbor.core :as cbor]
   [malli.core :as m]
   [malli.transform :as mt]
   [com.rpl.specter :as x]
   [taoensso.telemere :as tel])
  (:import
   [java.io ByteArrayOutputStream]
   [java.net StandardProtocolFamily UnixDomainSocketAddress]
   [java.nio ByteBuffer]
   [java.nio.channels SocketChannel]))

(def EvalRequest
  [:map {:encode/cbor (fn [m]
                        (x/transform [x/MAP-KEYS] name m))}
   [:eval.request/artifact bytes?]
   [:eval.request/stdin    bytes?]
   [:eval.request/argv     [:vector :string]]])

#_(m/encode EvalRequest {:eval.request/artifact nil} (mt/transformer {:name :cbor}))

(def EvalResponse
  [:map {:decode/cbor (fn [m]
                        (x/transform [x/MAP-KEYS] (fn [n] (keyword "eval.response" n)) m))}
   [:eval.response/exit   :int]
   [:eval.response/stdout :string]
   [:eval.response/stderr :string]])

#_(m/decode EvalResponse {"artifact" nil} (mt/transformer {:name :cbor}))

(defn encode
  [eval-request]
  (->> eval-request
       (m/assert EvalRequest)
       ((fn [x]
          (m/encode EvalRequest x (mt/transformer {:name :cbor}))))
       cbor/encode))

(defn decode
  [cbor-bytes]
  (->> (.toByteArray cbor-bytes)
       cbor/decode
       ((fn [x]
          (m/decode EvalResponse x (mt/transformer {:name :cbor}))))
       (m/assert EvalResponse)))

;; docs
;; https://github.com/firecracker-microvm/firecracker/blob/main/docs/vsock.md

(defn- read-line! [ch]
  (let [sb  (StringBuilder.)
        buf (ByteBuffer/allocate 1)]
    (loop []
      (.clear buf)
      (let [n (.read ch buf)]
        (cond
          (= n -1) (throw (ex-info "Channel closed unexpectedly during vsock handshake" {}))
          (= n 0)  (recur)
          :else
          (do
            (.flip buf)
            (let [b (.get buf)]
              (if (= b (byte \newline))
                (str sb)
                (do
                  (.append sb (char b))
                  (recur))))))))))

(def VsockContext
  [:map
   [:vsock/host-socket-path :string]
   [:vsock/guest-port :int]])

(defn vsock-request!
  [{:vsock/keys [host-socket-path guest-port]}
   eval-request]
  (let [;; repeat handshake until successful
        ch (loop []
             (let [ch (SocketChannel/open StandardProtocolFamily/UNIX)]
               (if (try
                     (.connect ch (UnixDomainSocketAddress/of host-socket-path))
                     (.write ch (ByteBuffer/wrap (.getBytes (str "CONNECT " guest-port "\n") "UTF-8")))
                     (read-line! ch)
                     true
                     (catch Exception _
                       (.close ch)
                       false))
                 ch
                 (do (Thread/sleep 5)
                     (recur)))))]
    (try
      (tel/event! ::vsock-connect {:level :debug})
      (tel/event! ::vsock-send {:level :debug
                                :data eval-request})

      ;; Send CBOR-encoded message
      (.write ch (ByteBuffer/wrap (encode eval-request)))

      ;; Read CBOR response
      (let [max-bytes (* 512 1024)
            baos     (ByteArrayOutputStream.)
            buf      (ByteBuffer/allocate 4096)]
        (loop [total 0]
          (let [n (.read ch buf)]
            (when (pos? n)
              (let [new-total (+ total n)]
                (when (> new-total max-bytes)
                  (throw (ex-info "Response exceeds maximum size" {:max-bytes max-bytes})))
                (.flip buf)
                (let [bytes (byte-array (.remaining buf))]
                  (.get buf bytes)
                  (.write baos bytes))
                (.clear buf)
                (recur new-total)))))
        (tel/event! ::vsock-receive {:level :debug})
        (decode baos))
      (finally
        (.close ch)))))
