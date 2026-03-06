(ns cyberleague.db.core
  (:require
   [bloom.commons.uuid :as uuid]
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

(defn transact!
  [txs]
  (d/transact *conn* txs))

(defn entity-exists?
  [[id-key id]]
  (boolean (d/q '[:find ?id .
                  :in $ ?id-key ?id
                  :where
                  [_ ?id-key ?id]]
                (d/db *conn*)
                id-key
                id)))

;; Users

(defn generate-token [] (random-uuid))

(defn create-user!
  [{:keys [id github-id name]}]
  (d/transact
   *conn*
   [{:user/id id
     :user/github-id github-id
     :user/name name
     :user/cli-token (generate-token)}]))

(defn reset-cli-token!
  [user-id]
  (let [token (generate-token)]
    (d/transact *conn*
                [[:db/add user-id :user/cli-token token]])
    token))

(defn token->user-id
  [token]
  (d/q '[:find ?e .
         :in $ ?token
         :where [?e :user/cli-token ?token]]
       (d/db *conn*)
       token))

(defn get-or-create-user!
  [github-id uname]
  (let [db (d/db *conn*)]
    (if-let [user-id (d/q '[:find ?id .
                            :in $ ?github-id
                            :where
                            [?e :user/github-id ?github-id]
                            [?e :user/id ?id]]
                          db
                          github-id)]
      user-id
      (create-user! {:id (uuid/random)
                     :github-id github-id
                     :name uname}))))

(defn random-user-id
  []
  (d/q '[:find ?id .
         :where
         [_ :user/id ?id]]
       (d/db *conn*)))

(defn get-users
  "Get all the user entities"
  []
  (let [db (d/db *conn*)]
    (->> (d/q '[:find ?e
                :where
                [?e :user/name _]]
              db)
         (map (comp (partial d/entity db) first)))))


;; Bots

(defn gen-bot-name []
  (str (apply str (take 3 (repeatedly #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))))
       "-"
       (+ 1000 (rand-int 8999))))

(defn init-code!
  [bot-id code language]
  @(d/transact *conn*
               [{:bot/id bot-id
                 :bot/code
                 {:code/id (uuid/random)
                  :code/code code
                  :code/language language}}]))

(defn update-code!
  [bot-id code]
  (let [code-eid (d/q '[:find ?code-eid .
                        :in $ ?bot-id
                        :where
                        [?b :bot/id ?bot-id]
                        [?b :bot/code ?code-eid]]
                      (d/db *conn*)
                      bot-id)]
    @(d/transact *conn*
                 [[:db/add code-eid :code/code code]])))

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
  (let [bot (by-id [:bot/id bot-id])
        code-timestamp (d/q '[:find ?tx .
                              :in $ ?cid
                              :where
                              [?cid :code/code _ ?tx]]
                            (d/db *conn*)
                            (get-in bot [:bot/code :db/id]))]
    (-> @(d/transact *conn*
                     [[:db/add [:bot/id bot-id] :bot/code-version code-timestamp]])
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

(defn bot-id [user-id bot-name]
  (d/q '[:find ?id .
         :in $ ?user-id ?bot-name
         :where
         [?e :bot/user ?user-id]
         [?e :bot/name ?bot-name]
         [?e :bot/id ?id]]
       (d/db *conn*)
       user-id
       bot-name))

(defn bot-history
  [bot-id]
  (->> (d/q '[:find ?inst ?attr ?val
              :in $ ?id
              :where
              [?e :bot/id ?id]
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
