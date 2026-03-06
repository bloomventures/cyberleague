(ns cyberleague.server.cqrs
  (:require
   [bloom.commons.uuid :as uuid]
   [tada.events.core :as tada]
   [cyberleague.coordinator.test-bot :as coordinator]
   [cyberleague.db.core :as db]
   [cyberleague.game-registrar :as registrar]
   [cyberleague.common.graph.core :as graph]))

(defonce t (tada/init :malli))

(def bot-pattern
  [:bot/name
   {:bot/user [:user/id
               :user/name]}
   :bot/id
   :bot/rating
   :bot/status
   {:bot/game [:game/id
               :game/name]}])

(defn entity-exists?-condition [id-key id]
  [#(db/entity-exists? [id-key id])
   :forbidden
   (str "Entity " id-key " " id " does not exist.")])

(defn user-owns-bot?-condition [user-id bot-id]
  [#(= user-id (:user/id (:bot/user (db/by-id [:bot/id bot-id]))))
   :forbidden
   (str "User " user-id " does not own bot " bot-id ".")])

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

   {:id :api/games
    :params {:user-id [:maybe :user/id]}
    :rest [:get "/api/games"]
    :return (fn [_]
              (-> (graph/pull
                   {}
                   [{:entity/game
                     [:game/id
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
              (graph/pull
               {:game/id game-id}
               [:game/id
                :game/name
                :game/description
                {:game/bots bot-pattern}]))}

   {:id :api/match
    :params {:user-id [:maybe :user/id]
             :match-id :match/id}
    :rest [:get "/api/matches/:match-id"]
    :conditions (fn [{:keys [match-id]}]
                  [(entity-exists?-condition :match/id match-id)])
    :return (fn [{:keys [match-id]}]
             (graph/pull
               {:match/id match-id}
               [:match/id
                {:match/game [:game/id
                              :game/name]}
                {:match/bots bot-pattern}
                {:match/winner [:bot/id]}
                :match/error
                :match/moves
                :match/std-out-history
                :match/state-history]))}

   {:id :api/bot-id-by-name
    :params {:user-id :user/id
             :bot-name :bot/name}
    :rest [:post "/api/bots/get-id"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :return (fn [{:keys [user-id bot-name]}]
              (when-let [bot-id (db/bot-id user-id bot-name)]
                {:bot-id bot-id}))}

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
                {:bot/game [:game/id
                            :game/name]}
                {:bot/user [:user/id
                             :user/name]}
                {:bot/code [:code/language]}
                {:bot/matches [:match/id
                               :match/error
                               :match/winner
                               {:match/bots bot-pattern}]}]))}

   {:id :api/bot-code
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:get "/api/bots/:bot-id/code"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)])
    :return (fn [{:keys [_user-id bot-id]}]
              (graph/pull
               {:bot/id bot-id}
               [:bot/id
                :bot/name
                {:bot/code [:code/code
                            :code/language]}
                {:bot/user [:user/id
                            :user/name]}
                {:bot/game [:game/id
                            :game/name]}]))}

   {:id :api/create-bot!
    :params {:user-id :user/id
             :game-id :game/id}
    :rest [:post "/api/games/:game-id/bot"]
    :conditions (fn [{:keys [user-id game-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :game/id game-id)])
    :effect (fn [{:keys [user-id game-id]}]
              (let [id (uuid/random)]
                (db/transact! [{:bot/id id
                                :bot/user [:user/id user-id]
                                :bot/game [:game/id game-id]
                                :bot/name (db/gen-bot-name)
                                :bot/rating 1500
                                :bot/rating-dev 350}])
                id))
    :return (fn [{id :tada/effect-return}]
              {:bot/id id})}

   {:id :api/set-bot-language!
    :params {:user-id :user/id
             :bot-id :bot/id
             :language :code/language}
    :rest [:put "/api/bots/:bot-id/language/:language"]
    :conditions (fn [{:keys [user-id bot-id _language]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(nil? (:code/language (:bot/code (db/by-id [:bot/id bot-id]))))]])
    :effect (fn [{:keys [_user-id bot-id language]}]
              (let [bot (db/by-id [:bot/id bot-id])
                    game-name (get-in bot [:bot/game :game/name])
                    code (get-in @registrar/games [game-name :game.config/starter-code language] "")]
                (db/init-code! bot-id code language)))}

   {:id :api/set-bot-code!
    :params {:user-id :user/id
             :bot-id :bot/id
             :code :code/code}
    :rest [:put "/api/bots/:bot-id/code"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(:code/language (:bot/code (db/by-id [:bot/id bot-id])))]])
    :effect (fn [{:keys [_user-id bot-id code]}]
              (db/update-code! bot-id code))}

   {:id :api/test-bot!
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:post "/api/bots/:bot-id/test"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(let [code (:bot/code (db/by-id [:bot/id bot-id]))]
                       (and (:code/code code) (:code/language code)))]])
    :effect (fn [{:keys [user-id bot-id]}]
              (coordinator/test-bot user-id
                                    bot-id
                                    (db/by-id [:bot/id bot-id])))
    :return :tada/effect-return}

   {:id :api/deploy-bot!
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:post "/api/bots/:bot-id/deploy"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(let [code (:bot/code (db/by-id [:bot/id bot-id]))]
                       (and (:code/code code) (:code/language code)))]])
    :effect (fn [{:keys [_user-id bot-id]}]
              (db/deploy-bot! bot-id))}])

(tada/register! t events)

#_(tada/do! t :api/user {:user-id 5})

;; NOTE: Also need to reload routes when changing this file.
#_(when (= :dev (cyberleague.config/config :environment))
    (require '[cyberleague.server.routes] :reload))
