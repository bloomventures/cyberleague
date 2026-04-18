(ns cyberleague.cli.subcommands.bot.watch
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [nextjournal.beholder :as beholder]
   [hyperfiddle.rcf :as rcf]
   [cyberleague.cli.util.remote :as r]))

(defonce watcher (atom nil))

(defn stop-watcher!
  []
  (beholder/stop @watcher))

(defn extract-bot-name [file-name]
  (some-> (re-find #"[A-Z]{3}[-_][0-9]{4}" file-name)
          (str/replace "_" "-")))

(rcf/tests
  (extract-bot-name "ABC_1234.clj") := "ABC-1234")

(defn get-bot-id [bot-name]
  (-> (r/tada! [:api/bot-id-by-name
                {:bot-name bot-name}])
      :bot-id))

(defn push! [path]
  (if-let [bot-name (extract-bot-name (str (.getFileName path)))]
    (if-let [bot-id (get-bot-id bot-name)]
      (do (println "Pushing " bot-name " code to the arena!")
          (r/tada! [:api/set-bot-code!
                    {:code (slurp (str path))}]))
      (println "Warning: Bot does not exist!"))
    (println "Ignoring file: Not a Bot")))

(defn watch-fn [{:keys [type path]}]
  (when (#{:modify :create} type)
    (push! path)))

(defn watch! [file-path]
  (if (.exists (io/file file-path))
    (do (reset! watcher (beholder/watch #'watch-fn file-path))
        (println "Watching files at:" file-path "👀")
        @(promise))
    (println "File does not exist!")))

(comment
  (cyberleague.cli.core/-main "bot" "watch" "./cli-demo/"))
