(ns cyberleague.schema
  (:require
   [malli.core :as m]
   [malli.registry :as mr]))

(def DbId :pos-int)

(def schema
  {:user {:user/id DbId}})

(mr/set-default-registry!
 (merge (mr/schemas m/default-registry)
        {:pos-int (m/-simple-schema {:type :pos-int :pred pos-int?})}
        (->> schema
             (mapcat val)
             (into {}))))
