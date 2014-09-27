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
                :db/valueType :db.type/long ; transaction id
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

(defn create-entity
  "Create a new entity with the given attributes and return the newly-created
  entity"
  [attributes]
  (let [new-id (d/tempid :entities)
        {:keys [db-after tempids]} @(d/transact *conn*
                                       [(assoc attributes :db/id new-id)])]
    (->> (d/resolve-tempid db-after tempids new-id)
         (d/entity db-after))))

;; Users

(defn create-user
  [token uname]
  (create-entity {:user/token token :user/name uname}))

(defn user-bots
  "Get a list of all bots"
  [user]
  (let []
    ))

;; Games

(defn create-game
  [name description rules]
  (create-entity {:game/name name
                  :game/description description
                  :game/rules rules}))

(defn games
  "Get all the game entities"
  []
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :where
                [?e :game/name _]]
              db)
         (map (comp (partial d/entity db) first)))))

;; Bots

(defn create-bot
  [user-id game-id]
  (create-entity {:bot/user user-id
                  :bot/game game-id}))

(defn update-bot-code
  [bot-id code]
  (let [bot (by-id bot-id)]
    (-> @(d/transact *conn*
                     (if-let [old-code (:bot/code bot)]
                       [[:db/add (:db/id old-code) :code/code code]]
                       (let [code-id (d/tempid :entities)]
                         [{:code/code code :db/id code-id}
                          [:db/add bot-id :bot/code code-id]])))
        :db-after
        (d/entity bot-id))))

(defn code-history
  [bot-id]
  (->> (d/q '[:find ?code ?tx
              :in $ ?cid
              :where
              [?cid :code/code ?code ?tx true]]
            (d/history (d/db *conn*))
            (get-in (by-id bot-id) [:bot/code :db/id]))
       (sort-by second)
       vec))

(defn deploy-bot
  [bot-id]
  (let [bot (by-id bot-id)
        code-timestamp (ffirst (d/q '[:find ?tx
                                      :in $ ?cid
                                      :where
                                      [?cid :code/code _ ?tx]]
                                    (d/db *conn*) (get-in bot [:bot/code :db/id])))]
    (-> @(d/transact *conn* [[:db/add bot-id :bot/code-version code-timestamp]])
        :db-after
        (d/entity bot-id))))

(defn deployed-code
  [bot-id]
  (let [bot (by-id bot-id)
        code-id (get-in bot [:bot/code :db/id])]
    (when-let [vers (:bot/code-version bot)]
      (-> (d/as-of (d/db *conn*) (d/t->tx vers))
          (d/entity code-id)
          :code/code))))
