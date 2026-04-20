(ns cyberleague.evaluator.artifacts
  "Storage and retrieval of artifacts"
  (:require
   [clojure.java.io :as io]
   [cyberleague.common.artifact :as artifact]))

(def artifact-dir-path "./artifacts")

(.mkdir (io/file artifact-dir-path))

(defn digest->file [digest]
  (io/file artifact-dir-path digest))

(defn exists? [digest]
  (.exists (digest->file digest)))

(defn store!
  [bytes]
  (let [hash (artifact/digest (io/input-stream bytes))]
    (with-open [out (io/output-stream (digest->file hash))]
      (io/copy (io/input-stream bytes) out))))

(defn load [digest]
  (slurp (digest->file digest)))
