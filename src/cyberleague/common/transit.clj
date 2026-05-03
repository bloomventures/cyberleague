(ns cyberleague.common.transit
  (:refer-clojure :exclude [read])
  (:require
   [cognitect.transit :as transit])
  (:import
   (java.io ByteArrayInputStream ByteArrayOutputStream)))

(defn read [stream]
  (transit/read (transit/reader stream :json)))

(defn read-str [transit-string]
  (when transit-string
    (let [in (ByteArrayInputStream. (.getBytes transit-string "UTF-8"))]
      (transit/read (transit/reader in :json)))))

(defn write-str [o]
  (when o
    (let [out (ByteArrayOutputStream. 4096)
          writer (transit/writer out :json)]
      (transit/write writer o)
      (.toString out))))

#_(read-str (write-str {:foo "bar"}))
