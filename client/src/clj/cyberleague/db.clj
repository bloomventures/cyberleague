(ns cyberleague.db
  (:require [datomic.api :as d]))

(def ^:dynamic *uri*
  "URI for the datomic database"
  "datomic:sql://cyberleague?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic")

(def ^:dynamic *conn* nil)

(defn init
  "Initialize the database connection"
  []
  (d/create-database *uri*)
  @(d/transact (d/connect *uri*)
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
               {:db/id #db/id [:db.part/db -4]
                :db/ident :game/rules
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/string
                :db.install/_attribute :db.part/db}

               ; bot
               {:db/id #db/id [:db.part/db -5]
                :db/ident :bot/user
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/ref
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -6]
                :db/ident :bot/game
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/ref
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -7]
                :db/ident :bot/code
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/ref
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -8]
                :db/ident :bot/code-version
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/instant
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -9]
                :db/ident :bot/rating
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/double
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -10]
                :db/ident :bot/rating-dev
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/double
                :db.install/_attribute :db.part/db}

               ; code
               {:db/id #db/id [:db.part/db -11]
                :db/ident :code/code
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/string
                :db.install/_attribute :db.part/db}

               ;user
               {:db/id #db/id [:db.part/db -12]
                :db/ident :user/token
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/string
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -13]
                :db/ident :user/name
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/string
                :db.install/_attribute :db.part/db}

               ;match
               {:db/id #db/id [:db.part/db -14]
                :db/ident :match/bots
                :db/cardinality :db.cardinality/many
                :db/valueType :db.type/ref
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -15]
                :db/ident :match/moves
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/string
                :db/doc "Stored as edn vector"
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -16]
                :db/ident :match/first-move
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/ref
                :db.install/_attribute :db.part/db}
               {:db/id #db/id [:db.part/db -17]
                :db/ident :match/winner
                :db/cardinality :db.cardinality/one
                :db/valueType :db.type/ref
                :db.install/_attribute :db.part/db}
               ]))

(defmacro with-conn
  "Execute the body with *conn* dynamically bound to a new connection."
  [& body]
  `(binding [*conn* (d/connect *uri*)]
     ~@body))

(defn by-id
  [eid]
  (d/entity (d/db *conn*) eid))

(defn create-user
  "Create a user, returning the id"
  [token uname]
  (let [new-id (d/tempid :entities)
        {:keys [db-after tempids]} @(d/transact *conn*
                                       [{:db/id new-id
                                         :user/token token
                                         :user/name uname}])]
    (->> (d/resolve-tempid db-after tempids new-id)
         (d/entity db-after))))
