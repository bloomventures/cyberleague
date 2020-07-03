(ns cyberleague.coordinator.db
  (:require [datomic.api :as d]))

(def ^:dynamic *uri*
  "URI for the datomic database"
  "datomic:mem://cyberleague-dev"
  #_"datomic:free://localhost:4334/cldev"
  #_"datomic:sql://cyberleague?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic"))

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
      {:db/id #db/id [:db.part/db -18]
       :db/ident :code/language
       :db/cardinality :db.cardinality/one
       :db/valueType :db.type/string
       :db.install/_attribute :db.part/db}

      ;user
      {:db/id #db/id [:db.part/db -12]
       :db/ident :user/gh-id
       :db/cardinality :db.cardinality/one
       :db/valueType :db.type/long
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
       :db/ident :match/winner
       :db/cardinality :db.cardinality/one
       :db/valueType :db.type/ref
       :db.install/_attribute :db.part/db}
      {:db/id #db/id [:db.part/db -17]
       :db/ident :match/error
       :db/cardinality :db.cardinality/one
       :db/valueType :db.type/boolean
       :db.install/_attribute :db.part/db}]))

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
  [gh-id uname]
  (create-entity {:user/gh-id gh-id :user/name uname}))

(defn get-or-create-user
  [gh-id uname]
  (let [db (d/db *conn*)]
    (if-let [user-id (first (first (d/q '[:find ?e
                                          :in $ ?gh-id
                                          :where [?e :user/gh-id ?gh-id]]
                                        db
                                        gh-id)))]
      (d/entity db user-id)
      (create-user gh-id uname))))

(defn get-user-bots
  "Get a list of all bots for a user"
  [user-id]
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :in $ ?p-id
                :where [?e :bot/user ?p-id]]
              db
              user-id)
         (map (comp (partial d/entity db) first)))))

(defn get-user [id]
  (by-id id))

(defn get-users
  "Get all the user entities"
  []
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :where
                [?e :user/name _]]
              db)
         (map (comp (partial d/entity db) first)))))

(defn get-user-bots [user-id]
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :in $ ?p-id
                :where [?e :bot/user ?p-id]]
              db
              user-id)
         (map (comp (partial d/entity db) first)))))
;; Games

(defn create-game
  [name description]
  (create-entity {:game/name name
                  :game/description description}))

(defn get-game [id]
  (by-id id))

(defn get-games
  "Get all the game entities"
  []
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :where
                [?e :game/name _]]
              db)
         (map (comp (partial d/entity db) first)))))


(defn get-game-bots [game-id]
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :in $ ?p-id
                :where [?e :bot/game ?p-id]]
              db
              game-id)
         (map (comp (partial d/entity db) first)))))

;; Bots


(defn gen-bot-name []
  (str (apply str (take 3 (repeatedly #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))))
       "-"
       (+ 1000 (rand-int 8999))))

(defn create-bot
  [user-id game-id]
  (create-entity {:bot/user user-id
                  :bot/game game-id
                  :bot/name (gen-bot-name)
                  :bot/rating 1500
                  :bot/rating-dev 350}))

(defn update-bot-code
  ([bot-id code] (update-bot-code bot-id code "clojurescript"))
  ([bot-id code language]
   (let [bot (by-id bot-id)]
     (-> @(d/transact *conn*
            (if-let [old-code (:bot/code bot)]
              [[:db/add (:db/id old-code) :code/code code]
               [:db/add (:db/id old-code) :code/language language]]
              (let [code-id (d/tempid :entities)]
                [{:code/code code :db/id code-id :code/language language}
                 [:db/add bot-id :bot/code code-id]])))
         :db-after
         (d/entity bot-id)))))

(defn update-bot-rating [bot-id rating rating-dev]
  @(d/transact *conn*
              [[:db/add bot-id :bot/rating rating]
               [:db/add bot-id :bot/rating-dev rating-dev]]))

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

(defn active-bots
  "Get all bots with deployed code"
  []
  (->> (d/q '[:find ?e
              :where
              [?e :bot/code-version _]]
            (d/db *conn*))
       (map (comp by-id first))
       (group-by :bot/game)))

(defn deployed-code
  [bot-id]
  (let [bot (by-id bot-id)
        code-id (get-in bot [:bot/code :db/id])]
    (when-let [vers (:bot/code-version bot)]
      (-> (d/as-of (d/db *conn*) vers)
          (d/entity code-id)
          :code/code))))


(defn get-code [id]
  (by-id id))

(defn get-bot [id]
  (by-id id))

(defn get-bot-matches [bot-id]
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :in $ ?p-id
                :where [?e :match/bots ?p-id]]
              db
              bot-id)
         (map (comp (partial d/entity db) first)))))

(defn get-bot-history
  [bot-id]
  (->> (d/q '[:find ?inst ?attr ?val
              :in $ ?e
              :where
              [?e ?a ?val ?tx true]
              [?a :db/ident ?attr]
              [?tx :db/txInstant ?inst]]
            (d/history (d/db *conn*))
            bot-id)
       (group-by first)
       (reduce-kv (fn [memo k v]
                    (let [rating (last (first (filter #(= :bot/rating (second %)) v)))
                          rating-dev (last (first (filter #(= :bot/rating-dev (second %)) v)))
                          [rating rating-dev]
                          (cond
                            (and (nil? rating) (nil? rating-dev)) [nil nil]
                            (nil? rating-dev) [rating
                                               (:bot/rating-dev (d/entity (d/as-of (d/db *conn*) k) bot-id))]
                            (nil? rating) [(:bot/rating (d/entity (d/as-of (d/db *conn*) k) bot-id))
                                           rating-dev]
                            :else [rating rating-dev])]
                      (if (and rating rating-dev)
                        (conj memo {:inst k :rating rating :rating-dev rating-dev})
                        memo))) [])
       (sort-by :inst)
       vec))

;; Matches

(defn get-match [id]
  (by-id id))
