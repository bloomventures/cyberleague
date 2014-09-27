(ns pog.db
  (:require [datomic.api :as d]
            [clojure.edn :as edn]))

;; Subset of code in client, just for connecting

(def ^:dynamic *uri*
  "URI for the datomic database"
  "datomic:sql://cyberleague?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic")

(def ^:dynamic *conn* nil)

(defmacro with-conn
  "Execute the body with *conn* dynamically bound to a new connection."
  [& body]
  `(binding [*conn* (d/connect *uri*)]
     ~@body))

(defn create-entity
  "Create a new entity with the given attributes and return the newly-created
  entity"
  [attributes]
  (let [new-id (d/tempid :entities)
        {:keys [db-after tempids]} @(d/transact *conn*
                                       [(assoc attributes :db/id new-id)])]
    (->> (d/resolve-tempid db-after tempids new-id)
         (d/entity db-after))))

(defn by-id
  [eid]
  (d/entity (d/db *conn*) eid))

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
      (-> (d/as-of (d/db *conn*) (d/t->tx vers))
          (d/entity code-id)
          :code/code
          edn/read-string))))
