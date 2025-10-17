(ns cyberleague.db.core
  (:require
   [datomic.api :as d]
   [cyberleague.config :as config]
   [cyberleague.db.schema :as schema]))

(def ^:dynamic *uri*
  "URI for the datomic database"
  (or (config/config :datomic-uri)
      (do (println "WARNING: Using in memory database. You're fine.")
          "datomic:mem://cyberleague"))
  #_"datomic:free://localhost:4334/cldev"
  #_"datomic:sql://cyberleague?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic")

(def ^:dynamic *conn* nil)

(defn init!
  "Initialize the database connection"
  []
  (d/create-database *uri*)
  @(d/transact
     (d/connect *uri*)
     schema/schema))

(defn drop! []
  (d/delete-database *uri*))

(defmacro with-conn
  "Execute the body with *conn* dynamically bound to a new connection."
  [& body]
  `(binding [*conn* (d/connect *uri*)]
     ~@body))

(defn by-id
  [eid]
  (d/entity (d/db *conn*) eid))

(defn create-entity!
  "Create a new entity with the given attributes and return the newly-created
  entity"
  [attributes]
  (let [new-id (d/tempid :entities)
        {:keys [db-after tempids]} @(d/transact *conn*
                                                [(assoc attributes :db/id new-id)])]
    (->> (d/resolve-tempid db-after tempids new-id)
         (d/entity db-after))))

(defn entity-exists? [id-key id]
  (let [entity (by-id id)]
   (and (some? id)
        ((id-key {:user/id :user/name
                  :bot/id :bot/name
                  :game/id :game/name
                  :match/id :match/bots}) entity))))

;; Users

(defn create-user!
  [github-id uname]
  (create-entity! {:user/github-id github-id
                  :user/name uname
                  :user/cli-token (random-uuid)}))

(defn generate-token [] (random-uuid))

(defn reset-cli-token!
  [user-id]
  (let [token (generate-token)]
    @(d/transact *conn* [[:db/add user-id :user/cli-token token]])
    token))

(defn token->user-id [token]
   (d/q '[:find ?e .
          :in $ ?token
          :where [?e :user/cli-token ?token]]
        (d/db *conn*)
        token))

(defn get-or-create-user
  [github-id uname]
  (let [db (d/db *conn*)]
    (if-let [user-id (first (first (d/q '[:find ?e
                                          :in $ ?github-id
                                          :where [?e :user/github-id ?github-id]]
                                        db
                                        github-id)))]
      (d/entity db user-id)
      (create-user! github-id uname))))

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

(defn create-game!
  [name description]
  (create-entity! {:game/name name
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

(defn create-bot!
  [user-id game-id]
  (create-entity! {:bot/user user-id
                  :bot/game game-id
                  :bot/name (gen-bot-name)
                  :bot/rating 1500
                  :bot/rating-dev 350}))

(defn update-bot-code!
  ([bot-id code] (update-bot-code! bot-id code "clojure"))
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

(defn deploy-bot!
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
          (select-keys [:code/code :code/language])))))

(defn get-code [id]
  (by-id id))

(defn get-bot-id [user-id bot-name]
  (let [db (d/db *conn*)]
    (d/q '[:find ?e .
           :in $ ?user-id ?bot-name
           :where
           [?e :bot/user ?user-id]
           [?e :bot/name ?bot-name]]
         db
         user-id
         bot-name)))

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

(defn update-rankings!
  [p1 p1r p1rd p2 p2r p2rd]
  (d/transact *conn*
              [[:db/add (:db/id p1) :bot/rating (Math/max 0 (Math/round p1r))]
               [:db/add (:db/id p2) :bot/rating (Math/max 0 (Math/round p2r))]
               [:db/add (:db/id p1) :bot/rating-dev (Math/max 50 (Math/round p1rd))]
               [:db/add (:db/id p2) :bot/rating-dev (Math/max 50 (Math/round p2rd))]]))

(defn disable-bot!
  [errd-bot]
  (with-conn
    (d/transact *conn*
                [[:db/retract (:db/id errd-bot) :bot/code-version (:bot/code-version errd-bot)]])))

(defn disable-cheater!
  [cheater]
  (d/transact *conn*
              [[:db/add (:db/id cheater) :bot/rating (Math/max 0 (- (:bot/rating cheater) 10))]
              (disable-bot! cheater)]))
