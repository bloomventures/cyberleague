(ns cyberleague.common.envs
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]))

(defn all []
  (let [d (edn/read-string (slurp (io/resource "cyberleague/evaluator/envs.edn")))]
    (zipmap (map :env/slug d)
            d)))
