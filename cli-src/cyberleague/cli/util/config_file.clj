(ns cyberleague.cli.util.config-file
  (:refer-clojure :exclude [read])
  (:require
   [clojure.java.io :as io]
   [cyberleague.cli.util.ednf :as ednf]))

(def filename "cyberleague.conf.edn")

(def Config
  [:map
   [:cyberleague.cli.config/token :string]
   [:cyberleague.cli.config/api-server-url {:optional true} :string]])

(defn- find-path
  []
  (->> (.getAbsoluteFile (io/file ""))
       (iterate (fn [dir] (.getParentFile dir)))
       (take-while some?)
       (map (fn [dir] (io/file dir filename)))
       (filter (fn [^java.io.File f] (.exists f)))
       first))

(defn read
  []
  (when-let [^java.io.File f (find-path)]
    (ednf/read (.getPath f))))

(defn set-kv!
  [k v]
  (let [p (or (when-let [^java.io.File f (find-path)] (.getPath f))
              filename)]
    (-> (read)
        (assoc k v)
        (->> (ednf/write! p)))))
