(ns cyberleague.cli.util.bot-dev
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [malli.core :as m]
   [malli.error :as me]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.cli.util.weight :as weight]
   [cyberleague.cli.util.remote :as r]
   [cyberleague.common.transit-client :as http]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.bot-config :as bot-config]))

(defn ->artifact
  [bot-config]
  (let [path (:bot/build-artifact bot-config)
        ^java.io.File artifact (io/file (bot-config/dir bot-config) path)]
    (if (.exists artifact)
      artifact
      (throw (ex-info (str "Artifact not found at expected path: " path) {})))))

(defn build!
  [bot-config]
  (println (f/color :color/yellow "Building..."))
  (if-let [cmd (:bot/build-cmd bot-config)]
    (do
      (println ">" cmd)
      (let [windows? (-> (System/getProperty "os.name") .toLowerCase (.contains "win"))
            shell    (if windows? ["cmd.exe" "/c"] ["/bin/sh" "-c"])
            {:keys [exit out err]} (apply sh/sh (concat shell [cmd :dir (bot-config/dir bot-config)]))]
        (println out)
        (when (not= 0 exit)
          (throw (ex-info (str "Build failed (exit " exit "):\n" err) {})))
        (when-let [artifact (->artifact bot-config)]
          (println "Build successful.")
          (println "Digest:" (artifact/digest artifact)))))
    (println "No build command, skipping.")))

(defn upload!
  [bot-config]
  (println (f/color :color/yellow "Uploading..."))
  (let [artifact (->artifact bot-config)
        ;; by having this in the CLI,
        ;; we are effectively letting bots "self report" their weight
        ;; (in theory, one could hit this endpoint directly)
        ;; we don't want to use build file weights b/c it punishes
        ;; langs that have to statically link
        ;; for now, this is fine; one day maybe we'll do the builds
        ;; on our own servers
        weight (weight/dir-weight (io/file (bot-config/dir bot-config)))
        _ (println "Digest:" (artifact/digest artifact))
        _ (println "Weight:" weight)
        response (r/tada! [:api/artifact-upload-prepare!
                           {:bot-id (:bot/id bot-config)
                            :env-slug (:bot/env bot-config)
                            :digest (artifact/digest artifact)
                            :weight weight}])]
    (cond
      (:skip? response)
      (println "Artifact already exists. Skipping.")

      (:upload-url response)
      (do
        (let [upload-url (:upload-url response)]
          (println "Received upload path. Uploading...")
          (try
            (http/file-upload-request
             {:url upload-url
              :method :post
              :body artifact})
            (println "Upload successful.")
            (catch Exception _
              (println "Upload error.")))))

      :else
      (throw (ex-info "Error uploading" {})))))

(defn test-remote!
  [bot-config]
  (println (f/color :color/yellow "Testing..."))
  (let [artifact (->artifact bot-config)]
    (println "Digest:" (artifact/digest artifact))
    (if-let [match-id (:match/id (r/tada! ^{:timeout 120000}
                                          [:api/test-bot!
                                           {:bot-id (:bot/id bot-config)
                                            :digest (artifact/digest artifact)}]))]
      (println "Test completed. View it online.")
      (println "Error running test."))))



(defn deploy!
  [bot-config]
  (println (f/color :color/yellow "Deploying..."))
  (let [artifact (->artifact bot-config)]
    (r/tada! [:api/deploy-bot!
              {:bot-id (:bot/id bot-config)
               :digest (artifact/digest artifact)}])
    (println "Deploy successful.")))

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

#_(test-local! (bot-config/read! "."))

(defn stage!
  [bot-config]
  (build! bot-config)
  (test-local! bot-config)
  (upload! bot-config)
  (test-remote! bot-config))
