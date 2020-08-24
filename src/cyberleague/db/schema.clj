(ns cyberleague.db.schema)


(def schema
  [; partition for our data
   {:db/id #db/id [:db.part/db -1]
    :db/ident :entities
    :db.install/_partition :db.part/db}

   ; games
   {:db/id #db/id [:db.part/db -2]
    :db/ident :game/name
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -3]
    :db/ident :game/description
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db.install/_attribute :db.part/db}

   ; bot
   {:db/id #db/id [:db.part/db -4]
    :db/ident :bot/user
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -5]
    :db/ident :bot/game
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -6]
    :db/ident :bot/code
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -7]
    :db/ident :bot/code-version
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long ; transaction id
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -8]
    :db/ident :bot/rating
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -9]
    :db/ident :bot/rating-dev
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -10]
    :db/ident :bot/name
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db.install/_attribute :db.part/db}

   ; code
   {:db/id #db/id [:db.part/db -11]
    :db/ident :code/code
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -12]
    :db/ident :code/language
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db.install/_attribute :db.part/db}

   ;user
   {:db/id #db/id [:db.part/db -13]
    :db/ident :user/github-id
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -14]
    :db/ident :user/name
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db.install/_attribute :db.part/db}

   ;match
   {:db/id #db/id [:db.part/db -15]
    :db/ident :match/bots
    :db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -16]
    :db/ident :match/moves
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db/doc "Stored as edn vector"
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -17]
    :db/ident :match/state-history
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string
    :db/doc "Stored as edn vector"
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -18]
    :db/ident :match/winner
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref
    :db.install/_attribute :db.part/db}
   {:db/id #db/id [:db.part/db -19]
    :db/ident :match/error
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/boolean
    :db.install/_attribute :db.part/db}])
