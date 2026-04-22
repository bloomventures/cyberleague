(ns cyberleague.cli.util.weight
  (:require
   [clojure.java.io :as io])
  (:import
   (java.io ByteArrayOutputStream)
   (org.tukaani.xz LZMA2Options XZOutputStream)))

(defn- compress-bytes [^bytes data]
  (let [options (LZMA2Options. LZMA2Options/PRESET_MAX)
        baos (ByteArrayOutputStream.)]
    (with-open [xzos (XZOutputStream. baos options)]
      (.write xzos data))
    (.size baos)))

(def skip-files #{"bot.edn"})

(defn skip-file? [f]
  (contains? skip-files (.getName f)))

(defn dir-weight [dir]
  (->> (file-seq (io/file dir))
       (filter (fn [f]
                 (and (.isFile f)
                      (not (skip-file? f)))))
       (map (fn [f]
              (compress-bytes (.readAllBytes (io/input-stream f)))))
       (reduce +)))

#_(dir-weight "path/to/bot/dir")
