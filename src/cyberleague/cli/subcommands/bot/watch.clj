(ns cyberleague.cli.subcommands.bot.watch
  (:refer-clojure :exclude [run!])
  (:require
   [clojure.java.io :as io]
   [nextjournal.beholder :as beholder]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.bot-dev :as bot-dev]))

(defonce state (atom nil))
(defonce running? (atom false))
(defonce dirty? (atom false))

(defn stop-watcher!
  []
  (beholder/stop (::watcher @state)))

(defn run! []
  (when (compare-and-set! running? false true)
    (try
      (loop []
        (reset! dirty? false)
        (let [bot-config (bot-config/read! (::dir @state))]
          (println "-----------------")
          (bot-dev/stage! bot-config))
        (when @dirty?
          (recur)))
      (finally
        (reset! running? false)))))

(defn watch-fn [{:keys [type path]}]
  (println "Change:" type (.toString path))
  (when (#{:modify :create} type)
    (reset! dirty? true)
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

