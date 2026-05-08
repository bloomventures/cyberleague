(ns cyberleague.db.schema
  (:require
   [clojure.string]
   [malli.core :as m]
   [malli.registry :as mr]
   [dat.malli :as dm]
   [dat.schema :as ds]
   [cyberleague.common.schema :as schema :refer [Slug NonBlankString]]))

(def dat-schema
  {:entity/language
   {:language/id {:dat/type :db.type/uuid
                  :dat/unique :dat.unique/identity}
    :language/slug {:dat/type :db.type/string
                    :dat/unique :dat.unique/identity
                    :dat/spec Slug}}

   :entity/env
   {:env/id {:dat/type :db.type/uuid
             :dat/unique :dat.unique/identity}
    :env/slug {:dat/type :db.type/string
               :dat/unique :dat.unique/identity
               :dat/spec Slug}
    :env/language {:dat/rel [:dat.rel/one :entity/language :language/id]}}

   :entity/game
   {:game/id {:dat/type :db.type/uuid
              :dat/unique :dat.unique/identity}
    :game/slug {:dat/type :db.type/string
                :dat/unique :dat.unique/identity
                :dat/spec Slug}}

   :entity/user
   {:user/id {:dat/type :db.type/uuid
              :dat/unique :dat.unique/identity}
    :user/github-id {:dat/type :db.type/long}
    :user/name {:dat/type :db.type/string
                :dat/spec NonBlankString}
    :user/cli-token {:dat/type :db.type/uuid}}

   :entity/bot
   {:bot/id {:dat/type :db.type/uuid
             :dat/unique :dat.unique/identity}
    :bot/user {:dat/rel [:dat.rel/one :entity/user :user/id]}
    :bot/game {:dat/rel [:dat.rel/one :entity/game :game/id]}

    :bot/active-artifact {:dat/rel [:dat.rel/one :entity/artifact :artifact/id]}

    :bot/rating {:dat/type :db.type/long}
    :bot/rating-dev {:dat/type :db.type/long}
    :bot/rating-digest {:dat/type :db.type/string
                        :dat/doc "Digest of artifact that was used in the match that made this change"}

    :bot/name {:dat/type :db.type/string}
    :bot/matches-transit {:dat/type :db.type/string
                          :dat/no-history true
                          :dat/doc "Store as transit; see match-store"}}

   :entity/artifact
   {:artifact/id {:dat/type :db.type/uuid
                  :dat/unique :dat.unique/identity}
    :artifact/bot {:dat/rel [:dat.rel/one :entity/bot :bot/id]}
    :artifact/env {:dat/rel [:dat.rel/one :entity/env :env/id]}
    ;; not treating digest as unique/identity
    ;; high likelihood that digests will collide
    ;; b/c of starter templates
    :artifact/digest {:dat/type :db.type/string}
    :artifact/weight {:dat/type :db.type/long}
    :artifact/created-at {:dat/type :db.type/instant}}

   #_#_:entity/match
   {:match/id {:dat/type :db.type/uuid
               :dat/unique :dat.unique/identity}
    :match/timestamp {:dat/type :db.type/instant}
    :match/test? {:dat/type :db.type/boolean}
    :match/bots {:dat/rel [:dat.rel/many :entity/bot :bot/id]}
    :match/artifacts {:dat/rel [:dat.rel/many :entity/artifact :artifact/id]}
    :match/winning-bots {:dat/rel [:dat.rel/many :entity/bot :bot/id]}
    :match/disqualified-bots {:dat/rel [:dat.rel/many :entity/bot :bot/id]}
    :match/player-mappings-transit {:dat/type :db.type/string
                                    :dat/doc "Stored as transit"}
    :match/log-transit {:dat/type :db.type/string
                        :dat/doc "Stored as transit"}}})

(def schema
  (cons {:db/ident :entities
         :db.install/_partition :db.part/db}
        (ds/->db-schema :dat.db/datomic dat-schema)))

#_(require 'dat.schema :reload)

(defn- map-schema->attrs [map-schema]
  (->> (m/children map-schema)
       (map (fn [[k _ s]] [k s]))
       (into {})))

(mr/set-default-registry!
 (merge
  (dm/->malli-registry dat-schema)
  (map-schema->attrs schema/Eval)
  (map-schema->attrs schema/LogEntry)
  (map-schema->attrs schema/Match)
  {:schema/slug schema/Slug
   :schema/non-blank-string schema/NonBlankString
   :schema/digest schema/Digest
   :schema/eval schema/Eval
   :schema/log-entry schema/LogEntry
   :schema/match schema/Match}))
