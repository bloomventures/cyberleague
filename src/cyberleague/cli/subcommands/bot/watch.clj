(ns cyberleague.cli.subcommands.bot.watch
  (:refer-clojure :exclude [run!])
  (:require
   [clojure.java.io :as io]
   [nextjournal.beholder :as beholder]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.bot-dev :as bot-dev]))

(defonce state (atom nil))

(defn stop-watcher!
  []
  (beholder/stop (::watcher @state)))

(defn run! []
  (let [bot-config (bot-config/read! (::dir @state))]
    (do
      (println "---")
      (bot-dev/build! bot-config)
      (bot-dev/upload! bot-config)
      (bot-dev/test! bot-config))))

(defn watch-fn [{:keys [type path]}]
  (println "Change:" type (.toString path))
  (when (#{:modify :create} type)
    (run!)))

(defn exec!
  [{:keys [dir]}]
  (let [dir (io/file dir)]
    (when (bot-config/read! (io/file dir))
      (do (reset! state {::watcher (beholder/watch #'watch-fn (.getPath dir))
                         ::dir dir})
          (println "👀 Watching files at:" (.getPath dir))
          (run!)
          ;; to keep cli running
          @(promise)))))

