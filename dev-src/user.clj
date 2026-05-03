(ns user
  (:require
   [hyperfiddle.rcf :as rcf]
   [clojure.pprint]
   [taoensso.telemere :as tel]))

(set! *warn-on-reflection* true)

(tel/add-handler!
 ::tap
 (fn
   ([signal] (tap> signal))
   ([])))

(tel/set-min-level! :debug)

(rcf/enable!)
