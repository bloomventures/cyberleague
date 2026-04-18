(ns cyberleague.cli.util.bot-dev
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [cyberleague.cli.util.remote :as r]
   [cyberleague.cli.util.bot-config :as bot-config]))

(defn build!
  [bot-config]
  (println "Building...")
  (if-let [cmd (:bot.build/cmd (:bot/build bot-config))]
    (do
      (println ">" cmd)
      (sh/sh cmd)
      (println "Build succesful."))
    (println "No build command, skipping.")))

(defn upload!
  [bot-config]
  (println "Uploading...")
  (let [path (:bot.build/artifact (:bot/build bot-config))
        artifact (io/file (bot-config/dir bot-config) path)]
    (if (.exists artifact)
      (r/tada! [:api/set-bot-code!
                {:bot-id (:bot/id bot-config)
                 :code (slurp (.getPath artifact))}])
      (println "Could not find artifact at " path)))
  (println "Upload succesful."))

(defn test!
  [bot-config]
  (println "Testing...")
  (println "(TODO)")
  #_(println "Test succesful."))

(defn deploy!
  [bot-config]
  (println "Deploy...")
  (println "(TODO)")
  #_(println "Deploy succesful."))
