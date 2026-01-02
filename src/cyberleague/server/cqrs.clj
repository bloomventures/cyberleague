(ns cyberleague.server.cqrs
  (:require
   [clojure.edn :as edn]
   [cyberleague.db.core :as db]
   [cyberleague.game-registrar :as registrar]
   [cyberleague.coordinator.test-bot :as coordinator]
   [cyberleague.schema :as schema] ;; To load custom schema
   [tada.events.core :as tada]))

(defonce t (tada/init :malli))

(defn entity-exists?-condition [id-key id]
  [#(db/entity-exists? id-key id)
   :forbidden
   (str "Entity " id-key " " id " does not exist.")])

(defn user-owns-bot?-condition [user-id bot-id]
  [#(= user-id (:db/id (:bot/user (db/get-bot bot-id))))
   :forbidden
   (str "User " user-id " does not own bot " bot-id ".")])

(def events
  [{:id :api/me
    :params {:user-id :user/id}
    :rest [:get "/api/user"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :return (fn [{:keys [user-id]}]
              (let [user (db/get-user user-id)]
                {:user/id (:db/id user)
                 :user/github-id (:user/github-id user)
                 :user/name (:user/name user)
                 :user/cli-token (:user/cli-token user)}))}
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
    :return (fn [{:keys [user-id]}]
              (->> (db/get-users)
                   (map
                    (fn [user]
                      {:user/id (:db/id user)
                       :user/name (:user/name user)
                       :user/gh-id (:user/gh-id user)
                         ;; TODO: likely a better way to fetch counts in datomic
                       :user/bot-count (count
                                        (db/get-user-bots
                                         (:db/id user)))}))))}
   {:id :api/user
    :params {:user-id [:maybe :user/id]
             :other-user-id :user/id}
    :rest [:get "/api/users/:other-user-id"]
    :conditions (fn [{:keys [other-user-id]}]
                  [(entity-exists?-condition :user/id other-user-id)])
    :return (fn [{:keys [other-user-id]}]
              (let [user (db/get-user other-user-id)
                    bots (db/get-user-bots other-user-id)]
                {:user/id (:db/id user)
                 :user/name (:user/name user)
                 :user/bots (->> bots
                                 (map
                                  (fn [bot]
                                    {:bot/name (:bot/name bot)
                                     :bot/user (select-keys (:bot/user bot)
                                                            [:db/id :user/name])
                                     :bot/id (:db/id bot)
                                     :bot/rating (:bot/rating bot)
                                     :bot/status (if (:bot/code-version bot)
                                                   :active
                                                   :inactive)
                                     :bot/game (let [game (:bot/game bot)]
                                                 {:game/id (:db/id game)
                                                  :game/name
                                                  (:game/name game)})})))}))}
   {:id :api/games
    :params {:user-id [:maybe :user/id]}
    :rest [:get "/api/games"]
    :return (fn [_]
              (->> (db/get-games)
                   (map
                    (fn [game]
                      {:game/id (:db/id game)
                       :game/name (:game/name game)
                         ;; TODO: likely a better way to fetch counts in datomic
                       :game/bot-count (count
                                        (db/get-game-bots (:db/id game)))}))))}

   {:id :api/game
    :params {:user-id [:maybe :user/id]
             :game-id :game/id}
    :rest [:get "/api/games/:game-id"]
    :conditions (fn [{:keys [game-id]}]
                  [(entity-exists?-condition :game/id game-id)])
    :return (fn [{:keys [game-id]}]
              (let [game (db/get-game game-id)
                    bots (db/get-game-bots game-id)]
                {:game/id (:db/id game)
                 :game/name (:game/name game)
                 :game/description (:game/description game)
                 :game/bots (->> bots
                                 (map
                                  (fn [bot]
                                    {:bot/user (select-keys (:bot/user bot) [:db/id :user/name])
                                     :bot/name (:bot/name bot)
                                     :bot/rating (:bot/rating bot)
                                     :bot/status (if (:bot/code-version bot)
                                                   :active
                                                   :inactive)
                                     :bot/id (:db/id bot)})))}))}
   {:id :api/match
    :params {:user-id [:maybe :user/id]
             :match-id :match/id}
    :rest [:get "/api/matches/:match-id"]
    :conditions (fn [{:keys [match-id]}]
                  [(entity-exists?-condition :match/id match-id)])
    :return (fn [{:keys [match-id]}]
              (let [match (db/get-match match-id)]
                {:match/id (:db/id match)
                 :match/game (let [game (-> match :match/bots first :bot/game)]
                               {:game/name (:game/name game)
                                :game/id (:game/id game)})
                 :match/bots (map (fn [b]
                                    {:bot/id (:db/id b)
                                     :bot/status (if (:bot/code-version b)
                                                   :active
                                                   :inactive)
                                     :bot/user (select-keys (:bot/user b)
                                                            [:db/id :user/name])
                                     :bot/name (:bot/name b)})
                                  (:match/bots match))
                 :match/moves (edn/read-string (:match/moves match))
                 :match/state-history (edn/read-string
                                       (:match/state-history match))
                 :match/winner (:db/id (:match/winner match))}))}
   {:id :api/bot-id-by-name
    :params {:user-id :user/id
             :bot-name :bot/name}
    :rest [:post "/api/bots/get-id"]
    :conditions (fn [{:keys [user-id]}]
                  [(entity-exists?-condition :user/id user-id)])
    :return (fn [{:keys [user-id bot-name]}]
              (when-let [bot-id (db/get-bot-id user-id bot-name)]
                {:bot-id bot-id}))}
   {:id :api/bot
    :params {:user-id [:maybe :user/id]
             :bot-id :bot/id}
    :rest [:get "/api/bots/:bot-id"]
    :conditions (fn [{:keys [bot-id]}]
                  [(entity-exists?-condition :bot/id bot-id)])
    :return (fn [{:keys [bot-id]}]
              (let [bot (db/get-bot bot-id)
                    matches (db/get-bot-matches (:db/id bot))
                    history (db/get-bot-history (:db/id bot))]
                {:bot/id (:db/id bot)
                 :bot/name (:bot/name bot)
                 :bot/status (if (:bot/code-version bot)
                                                   :active
                                                   :inactive)
                 :bot/game (let [game (:bot/game bot)]
                             {:game/id (:db/id game)
                              :game/name (:game/name game)})
                 :bot/user (let [user (:bot/user bot)]
                             {:user/id (:db/id user)
                              :user/name (:user/name user)
                              :user/gh-id (:user/gh-id user)})
                 :bot/code {:code/language
                            (get-in bot [:bot/code :code/language])}
                 :bot/history history
                 :bot/matches (map
                               (fn [match]
                                 {:match/id (:db/id match)
                                  :match/bots (map (fn [b]
                                                     {:bot/id (:db/id b)
                                                      :bot/user (select-keys (:bot/user b) [:db/id :user/name])
                                                      :bot/status (if (:bot/code-version bot)
                                                   :active
                                                   :inactive)
                                                      :bot/name (:bot/name b)})
                                                   (:match/bots match))
                                  :match/winner (:db/id (:match/winner match))})
                               matches)}))}
   {:id :api/bot-code
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:get "/api/bots/:bot-id/code"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)])
    :return (fn [{:keys [user-id bot-id]}]
              (let [bot (db/get-bot bot-id)]
                {:bot/id (:db/id bot)
                 :bot/name (:bot/name bot)
                 :bot/code {:code/code (:code/code (:bot/code bot))
                            :code/language (:code/language (:bot/code bot))}
                 :bot/user (let [user (:bot/user bot)]
                             {:user/id (:db/id user)
                              :user/name (:user/name user)
                              :user/gh-id (:user/gh-id user)})
                 :bot/game (let [game (:bot/game bot)]
                             {:game/id (:db/id game)
                              :game/name (:game/name game)})}))}
   {:id :api/create-bot!
    :params {:user-id :user/id
             :game-id :game/id}
    :rest [:post "/api/games/:game-id/bot"]
    :conditions (fn [{:keys [user-id game-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :game/id game-id)])
    :effect (fn [{:keys [user-id game-id]}]
              (db/create-bot! user-id game-id))
    :return (fn [{bot :tada/effect-return}]
              {:id (:db/id bot)})}
   {:id :api/set-bot-language!
    :params {:user-id :user/id
             :bot-id :bot/id
             :language :code/language}
    :rest [:put "/api/bots/:bot-id/language/:language"]
    :conditions (fn [{:keys [user-id bot-id language]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(nil? (:code/language (:bot/code (db/get-bot bot-id))))]])
    :effect (fn [{:keys [user-id bot-id language]}]
              (let [bot (db/get-bot bot-id)
                    game-name (get-in bot [:bot/game :game/name])
                    code (get-in @registrar/games [game-name :game.config/starter-code language])
                    bot (db/update-bot-code! bot-id code language)]
                bot))
    :return (fn [{bot :tada/effect-return}]
              {:bot/code {:code/code (:code/code (:bot/code bot))
                          :code/language (:code/language (:bot/code bot))}})}
   {:id :api/set-bot-code!
    :params {:user-id :user/id
             :bot-id :bot/id
             :code :code/code}
    :rest [:put "/api/bots/:bot-id/code"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(:code/language (:bot/code (db/get-bot bot-id)))]])
    :effect (fn [{:keys [user-id bot-id code]}]
              (let [bot (db/get-bot bot-id)]
                (db/update-bot-code! (:db/id bot)
                                     code
                                     (:code/language (:bot/code bot)))))}
   {:id :api/test-bot!
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:post "/api/bots/:bot-id/test"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(let [code (:bot/code (db/get-bot bot-id))]
                       (and (:code/code code) (:code/language code)))]])
    :effect (fn [{:keys [user-id bot-id]}]
              (let [bot (db/get-bot bot-id)]
                (coordinator/test-bot user-id bot-id bot)))
    :return :tada/effect-return}
   {:id :api/deploy-bot!
    :params {:user-id :user/id
             :bot-id :bot/id}
    :rest [:post "/api/bots/:bot-id/deploy"]
    :conditions (fn [{:keys [user-id bot-id]}]
                  [(entity-exists?-condition :user/id user-id)
                   (entity-exists?-condition :bot/id bot-id)
                   (user-owns-bot?-condition user-id bot-id)
                   [#(let [code (:bot/code (db/get-bot bot-id))]
                       (and (:code/code code) (:code/language code)))]])
    :effect (fn [{:keys [user-id bot-id]}]
              (let [bot (db/get-bot bot-id)]
                (db/deploy-bot! (:db/id bot))))}])

(tada/register! t events)

#_(tada/do! t :api/user {:user-id 5})

;; NOTE: Also need to reload routes when changing this file.
#_(when (= :dev (cyberleague.config/config :environment))
    (require '[cyberleague.server.routes] :reload))
