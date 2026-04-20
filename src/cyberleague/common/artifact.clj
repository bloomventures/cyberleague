(ns cyberleague.common.artifact
  (:require
   [clj-commons.digest :as digest]))

(defn digest [f]
  (digest/sha-256 f))
