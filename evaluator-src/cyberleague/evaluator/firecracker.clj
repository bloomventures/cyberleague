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
   [java.nio.file FileSystems Files LinkOption]
   [java.nio.file.attribute PosixFileAttributeView PosixFilePermissions]
   [java.lang ProcessBuilder$Redirect]
   [java.util UUID]))

(def FirecrackerContext
  [:map
   [:firecracker/firecracker-executable-path :string]
   ;; jailer must be runnable as sudo
   ;; sudo chown root:root /vm/jailer
   ;; sudo chmod u+s /vm/jailer
   [:firecracker/jailer-executable-path :string]
   ;; jailer will create jails under
   ;; {jail-base-path}/firecracker/{UUID}/root/
   ;; must be rw by server process
   [:firecracker/jail-base-path :string]
   ;; uid and gid to run jailer process
   ;; for server to access the vsock
   ;; this needs to be the server process user
   ;; to get the ids:
   ;; id -u {user}
   ;; id -g {user}
   [:firecracker/jailer-uid :int]
   [:firecracker/jailer-gid :int]
   [:firecracker/timeout-seconds :int]
   [:firecracker/log-level :string]])

(defn chroot-dir-path [jail-base-path id]
  (str jail-base-path "/firecracker/" id))

(defn jail-root-path [jail-base-path id]
  (str (chroot-dir-path jail-base-path id) "/root"))

(defn prepare-jail!
  "Creates the jail root directory. Returns a closeable context with
   :firecracker/vm-id, :firecracker/jail-root, and :firecracker/socket-path.
   Hard-link any resources needed by Firecracker into the jail root before calling start!."
  [{:firecracker/keys [jail-base-path] :as context}]
  (let [id          (str (UUID/randomUUID))
        root        (jail-root-path jail-base-path id)
        socket-path (str root "/firecracker.socket")
        _           (.mkdirs (io/file root))
        _           (let [path  (.toPath (io/file root))
                          group (-> (FileSystems/getDefault)
                                    (.getUserPrincipalLookupService)
                                    (.lookupPrincipalByGroupName "firecracker"))
                          view  (Files/getFileAttributeView path PosixFileAttributeView
                                                            (into-array LinkOption []))]
                      (.setGroup view group)
                      (Files/setPosixFilePermissions path (PosixFilePermissions/fromString "rwxr-x---")))
        _           (tel/event! ::prepare-jail {:level :info :jail-root root})]
    (o/with-close-fn
     (assoc context
            :firecracker/vm-id       id
            :firecracker/jail-root   root
            :firecracker/socket-path socket-path)
     (fn [_]
       (tel/event! ::cleanup-jail {:level :info :jail-root root})
       (->> (io/file (chroot-dir-path jail-base-path id))
            file-seq
            reverse
            (run! (fn [f] (.delete f))))))))

(defn start!
  "Starts a firecracker process via the jailer. Returns a closeable map; use with with-open+.
   Call prepare-jail! first to set up resources inside the jail."
  [{:firecracker/keys [firecracker-executable-path jailer-executable-path jailer-uid jailer-gid
                       jail-base-path timeout-seconds log-level vm-id socket-path] :as context}]
  (let [cmd  ["timeout" "--kill-after=1s" (str timeout-seconds "s")
               jailer-executable-path
               "--id" vm-id
               "--exec-file" firecracker-executable-path
               "--uid" (str jailer-uid)
               "--gid" (str jailer-gid)
               "--chroot-base-dir" jail-base-path
               "--"
               "--api-sock" "/firecracker.socket"
               "--level" (or log-level "Info")]
        _    (tel/event! ::start {:level :info :socket-path socket-path})
        proc (-> (ProcessBuilder. ^java.util.List cmd)
                 (.redirectOutput ProcessBuilder$Redirect/INHERIT)
                 (.redirectError ProcessBuilder$Redirect/INHERIT)
                 (.start))]
    (o/with-close-fn
     (assoc context ::process proc)
     (fn [fc]
       (tel/event! ::close {:level :info :socket-path (:firecracker/socket-path fc)})
       (.destroy (::process fc))
       (.waitFor (::process fc))))))

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
