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
