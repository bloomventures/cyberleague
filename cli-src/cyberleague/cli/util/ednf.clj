(ns cyberleague.cli.util.ednf
  (:refer-clojure :exclude [read])
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [zprint.core :as zprint]))

(defn ->edn
  [data]
  ;; use zprint, because pr-str has many issues
  ;; (ex. if logging at the same time, it gets interleaved in the print)
  (zprint/zprint-str
   data {:style :community
         :width 160
         :set {:sort? true}
         :map {:comma? false
               :sort? true
               :lift-ns? false
               :force-nl? true}}))

(defn read [path]
  (let [^java.io.File f (io/file path)]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defn write!
  [path value]
  (spit path (->edn value)))
