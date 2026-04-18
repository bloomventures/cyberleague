(ns cyberleague.cli.util.bot-config
  (:refer-clojure :exclude [read])
  (:require
   [clojure.java.io :as io]
   [malli.core :as m]
   [cyberleague.cli.util.ednf :as ednf]))

(def BotConfig
  [:map
   [:bot/env :string]
   [:bot/game :string]
   [:bot/id :uuid]
   [:bot/name :string]
   [:bot/build
    [:map
     [:bot.build/cmd :string]
     [:bot.build/artifact :string]]]])

(defn read!
  [dir]
  (let [f (io/file dir "bot.edn")]
    (if (.exists f)
      (let [v (ednf/read f)]
        (if (m/validate BotConfig v)
          (with-meta v {::file f})
          (throw (ex-info (str "Error in bot.edn: " (m/explain BotConfig v)) {}))))
      (throw (ex-info "bot.edn was not found in this directory." {})))))

(defn dir [bot-config]
  (.getParent (::file (meta bot-config))))
