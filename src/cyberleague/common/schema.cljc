(ns cyberleague.common.schema
  (:require
   [clojure.string]))

(def Slug
  [:re #"^[a-z0-9-]+$"])

(def NonBlankString
  [:fn {:error/message {:en "must not be blank"}}
   #(not (clojure.string/blank? %))])

(def Digest ;; sha-256
  [:re #"^[a-f0-9]{64}$"])

(def BotId
  :uuid)

(def PlayerId
  :int)

(def Eval
  [:map
   [:eval/stdout {:optional true} :string]
   [:eval/stderr {:optional true} :string]
   ;; stdout json->edn, "move"
   [:eval/return-value {:optional true} :any]
   [:eval/error {:optional true}
    [:or
     [:map
      [:eval.error/origin [:= :eval.error.origin/system]]
      [:eval.error/type [:enum
                         :eval.error.type/system-error
                         :eval.error.type/state-schema-invalid
                         :eval.error.type/context-schema-invalid]]]
     [:map
      [:eval.error/origin [:= :eval.error.origin/bot]]
      [:eval.error/type [:enum
                         :eval.error.type/invalid-json
                         :eval.error.type/invalid-move
                         :eval.error.type/illegal-move
                         :eval.error.type/failed-ping-pong]]]]]])

(def LogEntry
  [:map
   ;; state prior to moves; absent for abort entries (e.g. failed ping-pong)
   [:log-entry/state {:optional true} :any]
   [:log-entry/contexts {:optional true}
    [:map-of BotId :any]]
   [:log-entry/evals {:optional true}
    [:map-of BotId Eval]]])

(def Match
  [:map
   [:match/id :uuid]
   [:match/timestamp inst?]
   [:match/test? :boolean]
   [:match/bot-ids [:set :uuid]]
   [:match/game-id :uuid]
   [:match/artifact-ids [:set :uuid]]
   [:match/log [:vector LogEntry]]
   [:match/disqualified-bot-ids [:set :uuid]]
   ;; only in games that passed ping-pong handshake
   [:match/winning-bot-ids {:optional true} [:set :uuid]]
   [:match/player-mappings {:optional true}
    [:map-of
     BotId PlayerId]]])
