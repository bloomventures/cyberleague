(ns cyberleague.cli.config-file
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [zprint.core :as zprint]))

(def path "cyberleague-token.edn")

(defn read []
  (let [f (io/file path)]
    (when (.exists f)
      (edn/read-string (slurp f)))))

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

(defn write!
  [v]
  (spit path (->edn v)))

(defn set-kv!
  [k v]
  (-> (read)
      (assoc k v)
      (write!)))
