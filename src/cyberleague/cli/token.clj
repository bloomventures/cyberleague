(ns cyberleague.cli.token
  (:refer-clojure :exclude [read])
  (:require
   [cyberleague.cli.config-file :as config-file]))

(def re
  #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")

(defn read []
  (or (:cyberleague.cli.config/token
       (config-file/read))
      (throw (ex-info "Token Not Set" {}))))

(defn save!
  [v]
  (config-file/set-kv!
   :cyberleague.cli.config/token v))


