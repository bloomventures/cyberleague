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

(defn skip-file? [^java.io.File f]
  (contains? skip-files (.getName f)))

(defn dir-weight [dir]
  (->> (file-seq (io/file dir))
       (filter (fn [^java.io.File f]
                 (and (.isFile f)
                      (not (skip-file? f)))))
       (map (fn [^java.io.File f]
              (compress-bytes (.readAllBytes ^java.io.InputStream (io/input-stream f)))))
       (reduce +)))

#_(dir-weight "path/to/bot/dir")
