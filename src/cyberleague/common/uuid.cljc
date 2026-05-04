(ns cyberleague.common.uuid
  (:require
   [bloom.commons.uuid :as uuid]))

(defn from-string [s]
  #?(:clj (uuid/from-email s)
     :cljs ""))

