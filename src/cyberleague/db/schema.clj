(ns cyberleague.db.schema
  (:require
   [clojure.string]
   [malli.registry :as mr]
   [dat.schema :as ds]))

(def NonBlankString
  [:fn {:error/message {:en "must not be blank"}}
   #(not (clojure.string/blank? %))])

(def dat-schema
  {:entity/game
   {:game/id {:dat/type :db.type/uuid
              :dat/unique :dat.unique/identity}
    :game/name {:dat/type :db.type/string
                :dat/spec NonBlankString}
    :game/description {:dat/type :db.type/string}
    :game/rules {:dat/type :db.type/string}}

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
    :bot/code {:dat/rel [:dat.rel/one :entity/code :code/id]}
    :bot/code-version {:dat/type :db.type/long}
    :bot/rating {:dat/type :db.type/long}
    :bot/rating-dev {:dat/type :db.type/long}
    :bot/name {:dat/type :db.type/string}}

   :entity/code
   {:code/id {:dat/type :db.type/uuid
              :dat/unique :dat.unique/identity}
    :code/code {:dat/type :db.type/string}
    :code/language {:dat/type :db.type/string
                    :dat/spec [:enum
                               "javascript"
                               "clojure"]}}

   :entity/match
   {:match/id {:dat/type :db.type/uuid
               :dat/unique :dat.unique/identity}
    :match/bots {:dat/rel [:dat.rel/many :entity/bot :bot/id]}
    :match/winner {:dat/rel [:dat.rel/one :entity/bot :bot/id]}
    :match/moves-edn {:dat/type :db.type/string
                      :dat/doc "Stored as edn vector"}
    :match/state-history-edn {:dat/type :db.type/string
                              :dat/doc "Stored as edn vector"}
    :match/std-out-history-edn {:dat/type :db.type/string
                                :dat/doc "Stored as edn vector"}
    :match/error-edn {:dat/type :db.type/string
                      :dat/doc "Stored as edn vector"}}})

(def schema
  (cons {:db/ident :entities
         :db.install/_partition :db.part/db}
        (ds/->db-schema :dat.db/datomic dat-schema)))

#_(require 'dat.schema :reload)

(malli.registry/set-default-registry!
 (dat.malli/->malli-registry dat-schema))
