(ns cyberleague.common.graph.core
  (:require
   [clojure.edn :as edn]
   [datomic.api :as d]
   [dat.graph]
   [cyberleague.common.bot-weight :as bw]
   [cyberleague.db.schema :as schema]
   [cyberleague.db.core :as db]))

(defn transform-resolver
  [source-key target-key f]
  {:dat.resolver/id target-key
   :dat.resolver/in #{source-key}
   :dat.resolver/out #{target-key}
   :dat.resolver/f (fn [in]
                     {target-key (f (get in source-key))})})

(defn alias-resolver [source-key target-key]
  (transform-resolver source-key target-key identity))

(def dynamic-resolvers
  [{:dat.resolver/id :bot/status
    :dat.resolver/in [:bot/code-version]
    :dat.resolver/out [:bot/status]
    :dat.resolver/f (fn [{:keys [bot/code-version]}]
                      {:bot/status (if code-version
                                     :active
                                     :inactive)})}

   {:dat.resolver/id :bot/weight
    :dat.resolver/in [{:bot/code [:code/code]}]
    :dat.resolver/out [:bot/weight]
    :dat.resolver/f (fn [{:keys [bot/code]}]
                      {:bot/weight (bw/code-weight (:code/code code))})}

   {:dat.resolver/id :match/game
    :dat.resolver/in [{:match/bots [:bot/game]}]
    :dat.resolver/out [:match/game]
    :dat.resolver/f (fn [{:keys [match/bots]}]
                      {:match/game (-> bots first :bot/game)})}

   {:dat.resolver/id :bot/history
    :dat.resolver/in [:bot/id]
    :dat.resolver/out [:bot/history]
    :dat.resolver/f (fn [{:keys [bot/id]}]
                      {:bot/history (db/bot-history id)})}

   (alias-resolver :bot/_user-count :user/bot-count)
   (alias-resolver :bot/_game-count :game/bot-count)

   (alias-resolver :match/_bots :bot/matches)
   (alias-resolver :bot/_game :game/bots)
   (alias-resolver :bot/_user :user/bots)

   (transform-resolver :match/moves-edn :match/moves edn/read-string)
   (transform-resolver :match/error-edn :match/error edn/read-string)
   (transform-resolver :match/state-history-edn :match/state-history edn/read-string)
   (transform-resolver :match/std-out-history-edn :match/std-out-history edn/read-string)

   ])

#_(require 'dat.schema :reload)
#_(require 'dat.graph :reload)

(def pull (dat.graph/make-pull
           schema/dat-schema
           {:dynamic-resolvers dynamic-resolvers
            :q d/q
            :pull d/pull
            :conn `db/*conn*
            :->db (fn [x]
                    (d/db @(resolve x)))}))

;; widget.id -> widget.attr
#_(cyberleague.db.core/with-conn
   (pull {:user/id #uuid "6384e2b2-184b-3bf5-8ecc-f10ca7a6563c"}
         [:user/name]))

;; widget.id -> widget.sprocket -> sprocket.attr
#_(cyberleague.db.core/with-conn
   (let [bot-id (d/q '[:find ?id .
                       :where
                       [_ :bot/id ?id]]
                     (d/db db/*conn*))]
     (pull {:bot/id bot-id}
           [:bot/name
            {:bot/user [:user/name]}])))

;; widget.id -> widget.reverse-sprocket -> sprocket.attr
#_(cyberleague.db.core/with-conn
   (pull {:user/id #uuid "6384e2b2-184b-3bf5-8ecc-f10ca7a6563c"}
         [:user/name
          {:bot/_user [:bot/name :bot/id]}]))

;; dynamic resolvers
#_(cyberleague.db.core/with-conn
   (let [bot-id (d/q '[:find ?id .
                       :where
                       [_ :bot/id ?id]]
                     (d/db db/*conn*))]
     (pull {:bot/id bot-id}
           [:bot/status])))

#_(cyberleague.db.core/with-conn
   (let [bot-id (d/q '[:find ?id .
                       :where
                       [_ :bot/id ?id]]
                     (d/db db/*conn*))]
     (pull {:bot/id bot-id}
           [:bot/weight])))

