(ns user
  (:require
   [hyperfiddle.rcf :as rcf]
   [clojure.pprint]
   [taoensso.telemere :as tel]))

(tel/add-handler!
 ::tap
 (fn
   ([signal] (tap> signal))
   ([])))

(tel/set-min-level! :debug)

(rcf/enable!)
