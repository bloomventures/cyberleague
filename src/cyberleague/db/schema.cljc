(ns cyberleague.db.schema
  (:require
   [clojure.string]
   [malli.registry :as mr]
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
    :bot/name {:dat/type :db.type/string}}

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

   :entity/match
   {:match/id {:dat/type :db.type/uuid
               :dat/unique :dat.unique/identity}
    :match/timestamp {:dat/type :db.type/instant}
    :match/test? {:dat/type :db.type/boolean}
    :match/bots {:dat/rel [:dat.rel/many :entity/bot :bot/id]}
    :match/winning-bots {:dat/rel [:dat.rel/many :entity/bot :bot/id]}
    :match/disqualified-bots {:dat/rel [:dat.rel/many :entity/bot :bot/id]}
    :match/log-transit {:dat/type :db.type/string
                        :dat/doc "Stored as transit"}}})

(def schema
  (cons {:db/ident :entities
         :db.install/_partition :db.part/db}
        (ds/->db-schema :dat.db/datomic dat-schema)))

#_(require 'dat.schema :reload)

(malli.registry/set-default-registry!
 (dat.malli/->malli-registry dat-schema))
