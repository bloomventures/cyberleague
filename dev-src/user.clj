(ns user
  (:require
   [clojure.pprint]
   [hyperfiddle.rcf :as rcf]
   [taoensso.telemere :as tel]
   [cyberleague.db.seed :as seed]
   [cyberleague.server.core :as server]
   [cyberleague.coordinator.core :as coordinator]
   #_[cyberleague.evaluator.core :as evaluator]))

(set! *warn-on-reflection* true)

(tel/add-handler!
 ::tap
 (fn
   ([signal] (tap> signal))
   ([])))

(tel/set-min-level! :debug)

(rcf/enable!)

(defn start! []
  #_(evaluator/start!)
  (seed/seed!)
  (server/start!)
  (coordinator/start!))

#_(start!)

(defn stop! []
  (coordinator/stop!)
  (server/stop!)
  #_(evaluator/stop!))

#_(stop!)

