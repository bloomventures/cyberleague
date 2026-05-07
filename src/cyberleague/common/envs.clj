(ns cyberleague.common.envs
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [malli.core :as m]
   [cyberleague.common.schema :as schema]))

(def EnvConfigRaw
  [:map
   [:env/slug schema/Slug]
   [:env/language-slug schema/Slug]
   [:env/enabled? :boolean]
   [:env/runtime :keyword]
   [:env/run-cmd :string]
   [:env/build-cmd [:maybe :string]]
   [:env/artifact-path :string]
   [:env/argv [:vector :string]]
   [:env/note [:maybe :string]]
   [:env/status [:maybe :string]]])

(defn read-envs []
  (let [envs (->> (io/file "envs")
                  (file-seq)
                  (filter (fn [f]
                            (= "env.edn" (.getName f))))
                  (map (fn [f]
                         (edn/read-string (slurp f))))
                  (filter (fn [env]
                            (m/validate EnvConfigRaw env)))
                  (filter :env/enabled?))]
    (zipmap (map :env/slug envs)
            envs)))

#_(keys (read-envs))

(defonce *store (atom (read-envs)))

(defn reload! []
  (reset! *store (read-envs)))

(defn all []
  (vals @*store))

(defn by-slug [env-slug]
  (get @*store env-slug))

(defn enabled? [env-slug]
  (contains? @*store env-slug))

(defn files-for
  [env-slug]
  {:pre [(enabled? env-slug)]}
  (let [dir (io/file "envs" env-slug)]
    (->> dir
         (file-seq)
         (remove (fn [f]
                   (.isDirectory f)))
         (remove (fn [f]
                   (= "env.edn" (.getName f))))
         (map (fn [f]
                [(str (.relativize (.toPath dir) (.toPath f))) (slurp f)]))
         (into {}))))

#_(files-for "clojure-lein-uberjar")
