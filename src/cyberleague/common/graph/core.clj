(ns cyberleague.common.graph.core
  (:require
   [cheshire.core :as json]
   [clojure.set :as set]
   [datomic.api :as d]
   [dat.graph]
   [cyberleague.game-registrar :as games]
   [cyberleague.common.envs :as envs]
   [cyberleague.common.transit :as t]
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
    :dat.resolver/in [:bot/active-artifact]
    :dat.resolver/out [:bot/status]
    :dat.resolver/f (fn [{:keys [bot/active-artifact]}]
                      {:bot/status (if active-artifact
                                     :active
                                     :inactive)})}

   {:dat.resolver/id :bot/weight
    :dat.resolver/in [{:bot/active-artifact [:artifact/weight]}]
    :dat.resolver/out [:bot/weight]
    :dat.resolver/f (fn [{:keys [bot/active-artifact]}]
                      {:bot/weight (:artifact/weight active-artifact)})}

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
   (alias-resolver :env/_language :language/envs)

   (transform-resolver :match/log-transit :match/log t/read-str)
   (transform-resolver :match/player-mappings-transit :match/player-mappings t/read-str)

   (transform-resolver :env/slug :env/starter-files envs/files-for)

   {:dat.resolver/id ::env-from-edn-configs
    :dat.resolver/in [:env/slug]
    :dat.resolver/out [:env/language-slug
                       :env/enabled?
                       :env/run-cmd
                       :env/build-cmd
                       :env/artifact-path
                       :env/argv
                       :env/note]
    :dat.resolver/f (fn [{:keys [env/slug]}]
                      (envs/by-slug slug))}

   {:dat.resolver/id ::game-config
    :dat.resolver/in [:game/slug]
    :dat.resolver/out [:game/name
                       :game/description
                       :game/rules
                       :game/technical-notes
                       :game/context-spec
                       :game/context-example
                       :game/move-spec
                       :game/move-example]
    :dat.resolver/f (fn [{:keys [game/slug]}]
                      (-> (games/by-slug slug)
                          (dissoc
                           :game.config/slug
                           :game.config/state-spec
                           :game.config/match-results-view)
                          (set/rename-keys
                           {:game.config/name :game/name
                            :game.config/description :game/description
                            :game.config/rules :game/rules
                            :game.config/technical-notes :game/technical-notes
                            :game.config/context-spec :game/context-spec
                            :game.config/context-example :game/context-example
                            :game.config/move-spec :game/move-spec
                            :game.config/move-example :game/move-example})
                          (update :game/move-example json/generate-string)
                          (update :game/context-example json/generate-string)))}])

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

