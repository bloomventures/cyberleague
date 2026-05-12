(ns migrations
  (:require
   [datomic.api :as d]
   [cyberleague.db.core :as db]
   [cyberleague.common.transit :as t]
   [cyberleague.db.matches :as matches]
   [cyberleague.common.schema :as s]))

(def ^:private old-error-type->new
  {:move.error.type/failed-ping-pong :eval.error.type/system-error})

(defn- coerce-eval-error [eval-map]
  (if-let [old-type (get-in eval-map [:eval/error :move.error/type])]
    (-> eval-map
        (update :eval/error dissoc :move.error/type)
        (assoc-in [:eval/error :eval.error/type]
                  (get old-error-type->new old-type :eval.error.type/system-error)))
    eval-map))

(defn- coerce-log-entry [entry]
  (if-let [evals (:log-entry/evals entry)]
    (assoc entry :log-entry/evals (update-vals evals coerce-eval-error))
    entry))

(defn- entity->match
  [e]
  (cond-> {:match/id (:match/id e)
           :match/timestamp (:match/timestamp e)
           :match/game-id (-> (:match/bots e)
                              first
                              :bot/game
                              :game/id)
           :match/test? (boolean (:match/test? e))
           :match/bot-ids (->> (:match/bots e)
                               (map :bot/id)
                               set)
           :match/artifact-ids (->> (:match/artifacts e)
                                    (map :artifact/id)
                                    set)
           :match/log (->> (t/read-str (:match/log-transit e))
                           (map coerce-log-entry)
                           vec)
           :match/disqualified-bot-ids (->> (:match/disqualified-bots e)
                                            (map :bot/id)
                                            set)}
    (:match/winning-bot-ids e)
    (assoc :match/winning-bot-ids
           (->> (:match/winning-bot-ids e)
                (map :bot/id)
                set))
    (:match/player-mappings-transit e)
    (assoc :match/player-mappings
           (t/read-str (:match/player-mappings-transit e)))))


(defn m2026-05-08-add-bot-matches-transit-attr []
  (db/with-conn
    @(db/transact! [{:db/ident :bot/matches-transit
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/noHistory true
                     :db/doc "Store as transit; see match-store"}])))

(defn m2026-05-08-add-bot-rating-digest []
  (db/with-conn
   @(db/transact! [{:db/ident :bot/rating-digest
                    :db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one}])))

#_(spit "debug.edn" (pr-str (db/with-conn
 (let [db (d/db db/*conn*)]
   (->> (d/q '[:find [?e ...]
               :where
               [?e :match/id _]]
             db)
        (map (partial d/entity db))
        (map entity->match)
        (sort-by :match/timestamp)
        (take 2))))))

(defn m2026-05-08-wipe-bot-matches-transit []
  (db/with-conn
    (let [db (d/db db/*conn*)
          txs (->> (d/q '[:find ?b ?v
                          :where
                          [?b :bot/matches-transit ?v]]
                        db)
                   (map (fn [[eid v]] [:db/retract eid :bot/matches-transit v])))]
      (println "Retracting" (count txs) "bot/matches-transit values")
      @(db/transact! txs)
      nil)))


(defn bot-matches-transit-tx
  [bot-id matches]
  {:bot/id bot-id
   :bot/matches-transit (->> matches
                             (sort-by :match/timestamp #(compare %2 %1))
                             (take matches-to-store-per-bot)
                             t/write-str)})

(defn m2026-05-08-reified-matches-to-bot-transit-matches []
  (db/with-conn
    (let [db (d/db db/*conn*)
          already-done? (fn [bot-eid]
                          (some? (:bot/matches-transit (d/entity db bot-eid))))
          bot-eids (->> (d/q '[:find [?b ...]
                               :where
                               [?b :bot/id _]]
                             db)
                        (remove already-done?))]
      (println "Bots to process:" (count bot-eids))
      (doseq [bot-eid bot-eids]
        (let [bot-id (:bot/id (d/entity db bot-eid))
              matches (->> (d/q '[:find [?e ...]
                                  :in $ ?b
                                  :where
                                  [?e :match/bots ?b]]
                                db bot-eid)
                           (map (fn [eid] (entity->match (d/entity db eid)))))]
          (println "Bot" bot-id "- matches:" (count matches))
          @(db/transact! [(matches/bot-matches-transit-tx bot-id matches)])))
      nil)))

#_(db/with-conn (->> (d/q '[:find [?e ...]
                            :where
                            [?e :bot/id _]]
                          (d/db db/*conn*))
                     (map (partial d/entity (d/db db/*conn*)))
                     (filter :bot/matches-transit)
                     (map :bot/matches-transit)
                     ))

(def ^:private admin-user-id #uuid "21232f29-7a57-35a7-8389-4a0e4a801fc3")

(defn m2026-05-10-deploy-bots-without-active-artifact []
  (db/with-conn
    (let [db (d/db db/*conn*)
          bot-infos (->> (d/q '[:find ?b ?bot-id ?user-id
                                :where
                                [?b :bot/id ?bot-id]
                                [?b :bot/user ?u]
                                [?u :user/id ?user-id]
                                (not [?b :bot/active-artifact _])]
                              db)
                         (remove (fn [[_ _ user-id]] (= user-id admin-user-id))))
          txs (->> bot-infos
                   (keep (fn [[bot-eid bot-id _]]
                           (let [most-recent-artifact
                                 (->> (d/q '[:find ?a ?created-at
                                             :in $ ?b
                                             :where
                                             [?a :artifact/bot ?b]
                                             [?a :artifact/created-at ?created-at]
                                             [?a :artifact/digest ?digest]]
                                           db bot-eid)
                                      (sort-by second #(compare %2 %1))
                                      first)]
                             (when most-recent-artifact
                               (let [[artifact-eid _] most-recent-artifact
                                     digest (:artifact/digest (d/entity db artifact-eid))]
                                 (println "Deploying bot" bot-id "with digest" digest)
                                 (db/deploy-bot-tx bot-id digest)))))))]
      (println "Bots to deploy:" (count txs))
      @(db/transact! txs)
      nil)))

(defn m2026-05-10-backfill-artifact-env []
  (db/with-conn
    (let [db (d/db db/*conn*)
          artifact-eids (d/q '[:find [?a ...]
                               :where
                               [?a :artifact/id]
                               (not [?a :artifact/env])]
                             db)
          txs (map (fn [eid]
                     {:db/id eid
                      :artifact/env [:env/slug "clojure-lein-jvm"]})
                   artifact-eids)]
      (println "Backfilling env for" (count txs) "artifacts")
      @(db/transact! txs)
      nil)))
