(ns cyberleague.cli.util.config-file
  (:refer-clojure :exclude [read])
  (:require
   [clojure.java.io :as io]
   [cyberleague.cli.util.ednf :as ednf]))

(def filename "cyberleague-token.edn")

(defn- find-path
  []
  (->> (.getAbsoluteFile (io/file ""))
       (iterate (fn [dir] (.getParentFile dir)))
       (take-while some?)
       (map (fn [dir] (io/file dir filename)))
       (filter (fn [f] (.exists f)))
       first))

(defn read
  []
  (when-let [f (find-path)]
    (ednf/read (.getPath f))))

(defn set-kv!
  [k v]
  (let [p (or (some-> (find-path) .getPath)
              filename)]
    (-> (read)
        (assoc k v)
        (->> (ednf/write! p)))))
