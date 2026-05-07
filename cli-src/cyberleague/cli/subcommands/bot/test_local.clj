(ns cyberleague.cli.subcommands.bot.test-local
  (:require
   [cheshire.core :as json]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [malli.core :as m]
   [malli.error :as me]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]))

(defn- pretty-json [s]
  (try
    (-> s json/parse-string (json/generate-string {:pretty true}))
    (catch Exception _ s)))

(defn- run-bot-locally!
  [{:keys [run-cmd dir input timeout-ms]}]
  (let [windows? (-> (System/getProperty "os.name") .toLowerCase (.contains "win"))
        shell-args (if windows? ["cmd.exe" "/c"] ["/bin/sh" "-c"])
        result-future (future
                        (apply sh/sh (concat shell-args [run-cmd] [:in input :dir dir])))
        result (deref result-future (or timeout-ms 10000) :timeout)]
    result))

(defn test-local!
  [bot-config]
  (println (f/color :color/yellow "Testing locally..."))
  (let [run-cmd (:bot/run-cmd bot-config)
        dir (bot-config/dir bot-config)]
    (println "  run-cmd:" run-cmd)

    ;; Step 1: Ping-pong handshake
    (println)
    (println "Step 1: Ping-pong handshake")
    (let [ping-id (str (java.util.UUID/randomUUID))
          ping-json (json/generate-string {:ping ping-id})
          _ (println "  Sending:\n" (pretty-json ping-json))
          result (run-bot-locally! {:run-cmd run-cmd
                                    :dir dir
                                    :input ping-json
                                    :timeout-ms 10000})]
      (cond
        (= :timeout result)
        (println (f/color :color/red "  FAIL") "timed out after 10 seconds")

        (not= 0 (:exit result))
        (do
          (println (f/color :color/red "  FAIL") "process exited with" (:exit result))
          (when (not (str/blank? (:err result)))
            (println "  stderr:" (str/trim (:err result)))))

        :else
        (try
          (let [response (json/parse-string (:out result) true)]
            (println "  Response:\n" (pretty-json (:out result)))
            (if (= (:pong response) ping-id)
              (println (f/color :color/green "  PASS"))
              (do
                (println (f/color :color/red "  FAIL") "expected {:pong" (str "\"" ping-id "\"") "}"))))
          (catch Exception _
            (println (f/color :color/red "  FAIL") "could not parse response as JSON")
            (println "  got:" (str/trim (:out result)))))))

    ;; Step 2: Context test
    (println)
    (println "Step 2: Context test")
    (let [game-slug (:bot/game bot-config)
          _ (println "  Fetching game context for" (str game-slug "..."))
          games (r/tada! [:api/games {}])
          game-id (->> games
                       (filter (fn [g] (= (:game/slug g) game-slug)))
                       first
                       :game/id)]
      (if-not game-id
        (println (f/color :color/red "  ERROR") "game not found:" game-slug)
        (let [game (r/tada! [:api/game {:game-id game-id}])
              context-json (:game/context-example game)
              move-spec (:game/move-spec game)]
          (println (f/color :color/green "  OK"))
          (println "  Context:\n" (pretty-json context-json))
          (let [result (run-bot-locally! {:run-cmd run-cmd
                                          :dir dir
                                          :input context-json
                                          :timeout-ms 10000})]
            (cond
              (= :timeout result)
              (println (f/color :color/red "  FAIL") "timed out after 10 seconds")

              (not= 0 (:exit result))
              (do
                (println (f/color :color/red "  FAIL") "process exited with" (:exit result))
                (when (not (str/blank? (:err result)))
                  (println "  stderr:" (str/trim (:err result)))))

              :else
              (do
                (println "  Response:\n" (pretty-json (:out result)))
                (when (not (str/blank? (:err result)))
                  (println "  stderr:" (str/trim (:err result))))
                (try
                  (let [response (json/parse-string (:out result))]
                    (if (m/validate move-spec response)
                      (println (f/color :color/green "  PASS") "valid move")
                      (let [errors (me/humanize (m/explain move-spec response))]
                        (println (f/color :color/red "  FAIL") "invalid move")
                        (println "  Errors:" errors))))
                  (catch Exception _
                    (println (f/color :color/red "  FAIL") "could not parse response as JSON")
                    (println "  got:" (str/trim (:out result)))))))))))))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (test-local! bot-config)))
