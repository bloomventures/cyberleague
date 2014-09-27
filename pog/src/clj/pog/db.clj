(ns pog.db
  (:require [datomic.api :as d]))

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

(defn by-id
  [eid]
  (d/entity (d/db *conn*) eid))

(defn deployed-code
  [bot-id]
  (let [bot (by-id bot-id)
        code-id (get-in bot [:bot/code :db/id])]
    (when-let [vers (:bot/code-version bot)]
      (-> (d/as-of (d/db *conn*) (d/t->tx vers))
          (d/entity code-id)
          :code/code))))
