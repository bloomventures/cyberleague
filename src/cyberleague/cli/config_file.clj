(ns cyberleague.cli.config-file
  (:refer-clojure :exclude [read])
  (:require
   [cyberleague.cli.ednf :as ednf]))

(def path "cyberleague-token.edn")

(defn read
  []
  (ednf/read path))

(defn set-kv!
  [k v]
  (-> (read)
      (assoc k v)
      (->> (ednf/write! path))))
