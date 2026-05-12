(ns cyberleague.server.cqrs
  (:require
   [bloom.commons.uuid :as uuid]
   [tada.events.core :as tada]
   [cyberleague.coordinator.test-bot :as coordinator]
   [cyberleague.db.core :as db]
   [cyberleague.common.graph.core :as graph]
   [cyberleague.server.evaluator-client :as eval-client]))

(defonce t (tada/init :malli))

(def Pattern
  ;; TODO flesh this out
  [:vector :any])

(def bot-pattern
  [:bot/name
   {:bot/user [:user/id
               :user/name]}
   :bot/id
   :bot/rating
   :bot/status
   :bot/weight
   {:bot/game [:game/id
               :game/slug
               :game/name]}])

(defn entity-exists?-condition [id-key id]
  [#(db/entity-exists? [id-key id])
   :forbidden
   (str "Entity " id-key " " id " does not exist.")])

(defn entity-not-exists?-condition [id-key id]
  [#(not (db/entity-exists? [id-key id]))
   :forbidden
   (str "Entity " id-key " " id " already exists.")])

(defn user-owns-bot?-condition [user-id bot-id]
  [#(= user-id (:user/id (:bot/user (db/by-id [:bot/id bot-id]))))
   :forbidden
   (str "User " user-id " does not own bot " bot-id ".")])

(defn bot-has-artifact?-condition [bot-id digest]
  [#(boolean (db/bot-digest->artifact-id
              {:bot-id bot-id
               :digest digest}))
   :forbidden
   "Bot has no artifact with this digest"])

(def events
  [{:id :api/me
    :params {:user-id :user/id}
    :rest [:get "/api/user"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :return (fn [{:keys [user-id]}]
              (graph/pull
               {:user/id user-id}
               [:user/id
                :user/github-id
                :user/name
                :user/cli-token]))}

   {:id :api/reset-cli-token!
    :params {:user-id :user/id}
    :rest [:put "/api/cli-token"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :effect (fn [{:keys [user-id]}]
              (db/reset-cli-token! user-id))
    :return (fn [{token :tada/effect-return}]
              {:user/cli-token token})}

   {:id :api/users
    :params {:user-id [:maybe :user/id]}
    :rest [:get "/api/users"]
    :return (fn [_]
              (-> (graph/pull
                   {}
                   [{:entity/user
                     [:user/id
                      :user/name
                      :user/github-id
                      :user/bot-count]}])
                  :entity/user))}

   {:id :api/user
    :params {:user-id [:maybe :user/id]
             :other-user-id :user/id}
    :rest [:get "/api/users/:other-user-id"]
    :conditions (fn [{:keys [other-user-id]}]
                  [(entity-exists?-condition :user/id other-user-id)])
    :return (fn [{:keys [other-user-id]}]
              (graph/pull
               {:user/id other-user-id}
               [:user/id
                :user/name
                {:user/bots
                 bot-pattern}]))}

   {:id :api/languages
    :params {:user-id [:maybe :user/id]}
    :rest [:get "/api/languages"]
    :return (fn [_]
              (-> (graph/pull
                   {}
                   [{:entity/language
                     [:language/id
                      :language/slug
                      {:language/envs
                       [:env/id
                        :env/slug
                        :env/status]}]}])
                  :entity/language))}

   {:id :api/env
    :params {:user-id [:maybe :user/id]
             :env-slug :env/slug
             :pull-pattern [:maybe Pattern]}
    :rest [:get "/api/envs/starter-files"]
    :conditions (fn [{:keys [user-id env-slug]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :env/slug env-slug)])
    :return (fn [{:keys [env-slug pull-pattern]}]
              (graph/pull
               {:env/slug env-slug}
               pull-pattern))}

   {:id :api/games
    :params {:user-id [:maybe :user/id]}
    :rest [:get "/api/games"]
    :return (fn [_]
              (-> (graph/pull
                   {}
                   [{:entity/game
                     [:game/id
                      :game/slug
                      :game/name
                      :game/bot-count]}])
                  :entity/game))}

   {:id :api/game
    :params {:user-id [:maybe :user/id]
             :game-id :game/id}
    :rest [:get "/api/games/:game-id"]
    :conditions (fn [{:keys [game-id]}]
                  [(entity-exists?-condition :game/id game-id)])
    :return (fn [{:keys [game-id]}]
              (-> (graph/pull
                   {:game/id game-id}
                   [:game/id
                    :game/slug
                    :game/name
                    :game/description
                    :game/rules
                    :game/technical-notes
                    :game/context-spec
                    :game/context-example
                    :game/move-spec
                    :game/move-example])))}

   {:id :api/game-standings
    :params {:user-id [:maybe :user/id]
             :game-id :game/id}
    :rest [:get "/api/games/:game-id"]
    :conditions (fn [{:keys [game-id]}]
                  [(entity-exists?-condition :game/id game-id)])
    :return (fn [{:keys [game-id]}]
              (-> (graph/pull
                   {:game/id game-id}
                   [:game/id
                    :game/slug
                    :game/name
                    {:game/bots (conj bot-pattern
                                      :bot/ratings-recent)}])))}

   {:id :api/match
    :params {:user-id [:maybe :user/id]
             :match-id :match/id
             :bot-id :bot/id}
    :rest [:get "/api/matches/:match-id"]
    :conditions (fn [{:keys [bot-id]}]
                  [(entity-exists?-condition :bot/id bot-id)
                   ;; not checking match-id
                   ])
    :return (fn [{:keys [bot-id match-id]}]
              (graph/pull
               {:match/id match-id
                :bot/id bot-id}
               [:match/id
                {:match/game [:game/id
                              :game/name
                              :game/slug]}
                {:match/bots bot-pattern}
                {:match/artifacts [:artifact/digest
                                   {:artifact/bot [:bot/id]}]}
                {:match/winning-bots [:bot/id]}
                {:match/disqualified-bots [:bot/id]}
                :match/player-mappings
                :match/test?
                :match/timestamp
                :match/log]))}

   {:id :api/bot
    :params {:user-id [:maybe :user/id]
             :bot-id :bot/id}
    :rest [:get "/api/bots/:bot-id"]
    :conditions (fn [{:keys [bot-id]}]
                  [(entity-exists?-condition :bot/id bot-id)])
    :return (fn [{:keys [bot-id]}]
              (graph/pull
               {:bot/id bot-id}
               [:bot/id
                :bot/name
                :bot/status
                :bot/history
                :bot/weight
                {:bot/game [:game/id
                            :game/name
                            :game/slug]}
                {:bot/user [:user/id
                            :user/name]}
                {:bot/active-artifact [:artifact/digest
                                       :artifact/weight
                                       {:artifact/env
                                        [:env/slug
                                         {:env/language
                                          [:language/slug]}]}]}
                {:bot/artifacts [:artifact/digest
                                 :artifact/created-at]}
                {:bot/matches [:match/id
                               :match/test?
                               :match/timestamp
                               {:match/artifacts [:artifact/digest
                                                  {:artifact/bot [:bot/id]}]}
                               {:match/winning-bots [:bot/id]}
                               {:match/disqualified-bots [:bot/id]}
                               {:match/bots bot-pattern}]}]))}

   {:id :api/create-bot!
    :params {:user-id :user/id
             :game-slug :game/slug
             :env-slug :env/slug}
    :rest [:post "/api/bots"]
    :conditions (fn [{:keys [user-id game-slug env-slug]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :game/slug game-slug)
                   (entity-exists?-condition :env/slug env-slug)])
    :effect (fn [{:keys [user-id game-slug env-slug]}]
              (let [id (uuid/random)
                    bot-name (db/gen-bot-name)]
                (db/transact! [{:bot/id id
                                :bot/user [:user/id user-id]
                                :bot/game [:game/slug game-slug]
                                :bot/name bot-name
                                :bot/rating 1200
                                :bot/rating-dev 350}])
                {:bot/id id}))
    :return (fn [{result :tada/effect-return}]
              result)}

   {:id :api/artifact-upload-prepare!
    :params {:user-id :user/id
             :bot-id :bot/id
             :env-slug :env/slug
             :digest :artifact/digest
             :weight :artifact/weight}
    :rest [:get "/api/bots/artifacts/prepare"]
    :conditions (fn [{:keys [user-id bot-id env-slug]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (entity-exists?-condition :env/slug env-slug)
                   (user-owns-bot?-condition user-id bot-id)])
    :effect (fn [{:keys [_user-id bot-id env-slug digest weight]}]
              (if-let [result (eval-client/prepare {:digest digest})]
                (do (when-not (db/bot-digest->artifact-id
                               {:bot-id bot-id
                                :digest digest})
                      (db/transact!
                       [(db/artifact
                         {:bot-id bot-id
                          :env-slug env-slug
                          :digest digest
                          :weight weight})]))
                    result)
                (throw (ex-info "Upload failed, try again" {}))))
    :return :tada/effect-return}

   {:id :api/test-bot!
    :params {:user-id :user/id
             :bot-id :bot/id
             :digest :artifact/digest}
    :rest [:post "/api/bots/:bot-id/test"]
    :conditions (fn [{:keys [user-id bot-id digest]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   (bot-has-artifact?-condition bot-id digest)])
    :effect (fn [{:keys [user-id bot-id digest]}]
              (let [artifact-id (db/bot-digest->artifact-id
                                 {:bot-id bot-id
                                  :digest digest})]
                (coordinator/test-bot
                 {:bot-id bot-id
                  :artifact-id artifact-id})))
    :return :tada/effect-return}

   {:id :api/deploy-bot!
    :params {:user-id :user/id
             :bot-id :bot/id
             :digest :artifact/digest}
    :rest [:post "/api/bots/:bot-id/deploy"]
    :conditions (fn [{:keys [user-id bot-id digest]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   (bot-has-artifact?-condition bot-id digest)])
    :effect (fn [{:keys [_user-id bot-id digest]}]
              (when (db/artifact-changed? {:bot-id bot-id
                                           :digest digest})
                (db/transact!
                 [(db/deploy-bot-tx bot-id digest)])))}])

(tada/register! t events)

#_(tada/do! t :api/user {:user-id 5})

;; NOTE: Also need to reload routes when changing this file.
#_(when (= :dev (-> cyberleague.common.config/config :common :environment))
    (require '[cyberleague.server.routes] :reload))
