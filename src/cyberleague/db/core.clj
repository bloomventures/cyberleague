(ns cyberleague.db.core
  (:require
   [bloom.commons.uuid :as uuid]
   [datomic.api :as d]
   [cyberleague.common.config :as config]
   [cyberleague.db.schema :as schema]
   [dat.api :as dat]))

(def ^:dynamic *uri*
  "URI for the datomic database"
  (or (-> config/config
          :server
          :datomic-uri)
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

;; base entities

(defn env
  [{:keys [id slug language]}]
  {:env/id id
   :env/slug slug
   :env/language language})

(defn language
  [{:keys [id slug]}]
  {:language/id id
   :language/slug slug})

(defn artifact
  [{:keys [bot-id env-slug digest weight]}]
  {:artifact/id (dat/uuid)
   :artifact/bot [:bot/id bot-id]
   :artifact/env [:env/slug env-slug]
   :artifact/digest digest
   :artifact/weight weight
   :artifact/created-at (java.util.Date.)})

;; ---

(defn transact!
  [txs]
  (d/transact *conn* txs))

;; Artifacts

(defn bot-digest->artifact-id
  [{:keys [bot-id digest]}]
  (d/q '[:find ?artifact-id .
         :in $ ?bot-id ?digest
         :where
         [?b :bot/id ?bot-id]
         [?a :artifact/digest ?digest]
         [?a :artifact/bot ?b]
         [?a :artifact/id ?artifact-id]]
       (d/db *conn*)
       bot-id
       digest))

;; Envs and Languages

(defn env-slug->language-slug
  [env-slug]
  (d/q '[:find ?language-slug .
         :in $ ?env-slug
         :where
         [?e :env/slug ?env-slug]
         [?e :env/language ?l]
         [?l :language/slug ?language-slug]]
       (d/db *conn*)
       env-slug))

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
  (d/q '[:find ?id .
         :in $ ?token
         :where
         [?u :user/cli-token ?token]
         [?u :user/id ?id]]
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
  (apply str (take 3 (shuffle (vec "bcdfghjklmnpqrstvwxz")))))

(defn deploy-bot-tx
  [bot-id digest]
  (let [artifact-id (bot-digest->artifact-id {:bot-id bot-id
                                              :digest digest})]
    {:bot/id bot-id
     :bot/active-artifact [:artifact/id artifact-id]
     :bot/rating-dev 350}))

(defn dummy-bot
  [game-slug]
  (first (d/q '[:find ?b ?a
                :in $ ?game-slug ?admin-id
                :where
                [?u :user/id ?admin-id]
                [?g :game/slug ?game-slug]
                [?b :bot/user ?u]
                [?b :bot/game ?g]
                [?a :artifact/bot ?b]]
              (d/db *conn*)
              game-slug
              ;; special admin user-id
              #uuid "21232f29-7a57-35a7-8389-4a0e4a801fc3")))

(defn active-bots
  "Get all bots with deployed code"
  []
  (->> (d/q '[:find ?e
              :where
              [?e :bot/active-artifact _]]
            (d/db *conn*))
       (map (comp by-id first))
       (group-by :bot/game)))

(defn bot-history
  [bot-id]
  (let [history (d/history (d/db *conn*))
        raw (d/q '[:find ?tx ?inst ?attr ?val
                   :in $ ?id
                   :where
                   [?e :bot/id ?id]
                   [?e ?a ?val ?tx true]
                   [?a :db/ident ?attr]
                   [?tx :db/txInstant ?inst]]
                 history
                 bot-id)
        match-id-by-tx (->> (d/q '[:find ?tx ?match-id
                                   :in $ [?tx ...]
                                   :where
                                   [_ :match/id ?match-id ?tx true]]
                                 history
                                 (map first raw))
                            (into {}))]
    (->> raw
         (group-by second)
         (reduce-kv (fn [memo k v]
                      (let [rating (last (first (filter #(= :bot/rating (nth % 2)) v)))
                            rating-dev (last (first (filter #(= :bot/rating-dev (nth % 2)) v)))
                            tx (ffirst v)
                            [rating rating-dev]
                            (cond
                              (and (nil? rating) (nil? rating-dev)) [nil nil]
                              (nil? rating-dev) [rating
                                                 (:bot/rating-dev (d/entity (d/as-of (d/db *conn*) k) bot-id))]
                              (nil? rating) [(:bot/rating (d/entity (d/as-of (d/db *conn*) k) bot-id))
                                             rating-dev]
                              :else [rating rating-dev])]
                        (if (and rating rating-dev)
                          (conj memo {:inst k
                                      :rating rating
                                      :rating-dev rating-dev
                                      :match-id (match-id-by-tx tx)})
                          memo))) [])
         (sort-by :inst)
         vec)))

;; Matches

(defn disable-bot!
  [bot-id artifact]
  (d/transact *conn*
              [[:db/retract
                [:bot/id bot-id]
                :bot/active-artifact
                artifact]]))

(defn disable-cheater!
  [cheater]
  (d/transact *conn*
              [[:db/add [:bot/id (:bot/id cheater)] :bot/rating (Math/max 0 (- (:bot/rating cheater) 10))]
              (disable-bot! (:bot/id cheater)
                            (:bot/active-artifact cheater))]))
