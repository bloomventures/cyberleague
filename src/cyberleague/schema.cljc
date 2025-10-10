(ns cyberleague.schema
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [malli.registry :as mr]))

(def DbId :pos-int)
(def NonEmptyString [:re #"^.+$"])

(def schema
  {:user {:user/id DbId}
   :game {:game/id DbId}
   :match {:match/id DbId}
   :bot {:bot/id DbId
         :bot/name NonEmptyString}
   :code {:code/code :string
          :code/language [:enum "javascript" "clojure"]}})

(mr/set-default-registry!
 (merge (mr/schemas m/default-registry)
        {:pos-int (m/-simple-schema {:type :pos-int
                                     :pred pos-int?
                                     :type-properties {:decode/string mt/-string->long}
                                     })}
        (->> schema
             (mapcat val)
             (into {}))))

(comment
  (m/validate :bot/name "5")
  )
