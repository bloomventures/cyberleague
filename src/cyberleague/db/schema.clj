(ns cyberleague.db.schema)

(def schema
  [; partition for our data
   {:db/ident :entities
    :db.install/_partition :db.part/db}

   ; games
   {:db/ident :game/name
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}
   {:db/ident :game/description
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}

   ; bot
   {:db/ident :bot/user
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}
   {:db/ident :bot/game
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}
   {:db/ident :bot/code
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}
   {:db/ident :bot/code-version
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long ; transaction id
    }
   {:db/ident :bot/rating
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long}
   {:db/ident :bot/rating-dev
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long}
   {:db/ident :bot/name
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}

   ; code
   {:db/ident :code/code
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}
   {:db/ident :code/language
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}

   ;user
   {:db/ident :user/github-id
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long}
   {:db/ident :user/name
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}
   {:db/ident :user/cli-token
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/uuid}

   ;match
   {:db/ident :match/bots
    :db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}
   {:db/ident :match/moves
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db/doc "Stored as edn vector"}
   {:db/ident :match/state-history
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db/doc "Stored as edn vector"}
   {:db/ident :match/std-out-history
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db/doc "Stored as edn vector"}
   {:db/ident :match/winner
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}
   {:db/ident :match/error
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db/doc "Stored as edn vector"}])
