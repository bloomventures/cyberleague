(ns cyberleague.cli.util.bot-dev
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.cli.util.weight :as weight]
   [cyberleague.cli.util.remote :as r]
   [cyberleague.common.transit-client :as http]
   [cyberleague.cli.util.bot-config :as bot-config]))

(defn ->artifact
  [bot-config]
  (let [path (:bot.build/artifact (:bot/build bot-config))
        artifact (io/file (bot-config/dir bot-config) path)]
    (if (.exists artifact)
      artifact
      (throw (ex-info (str "Artifact not found at expected path: " path) {})))))

(defn build!
  [bot-config]

  (println "Building...")
  (if-let [cmd (:bot.build/cmd (:bot/build bot-config))]
    (do
      (println ">" cmd)
      (sh/sh cmd)
      (when (->artifact bot-config)
        (println "Build successful.")))
    (println "No build command, skipping.")))

(defn upload!
  [bot-config]
  (println "Uploading...")
  (let [artifact (->artifact bot-config)
        ;; by having this in the CLI,
        ;; we are effectively letting bots "self report" their weight
        ;; (in theory, one could hit this endpoint directly)
        ;; we don't want to use build file weights b/c it punishes
        ;; langs that have to statically link
        ;; for now, this is fine; one day maybe we'll do the builds
        ;; on our own servers
        weight (weight/dir-weight (io/file (bot-config/dir bot-config)))
        response (r/tada! [:api/artifact-upload-prepare!
                           {:bot-id (:bot/id bot-config)
                            :env-slug (:bot/env bot-config)
                            :digest (artifact/digest artifact)
                            :weight weight}])]
    (cond
      (:skip? response)
      (println "Artifact already exists. Skipping.")

      (:upload-url response)
      (let [upload-url (:upload-url response)
            upload-response (http/file-upload-request
                             {:url upload-url
                              :method :post
                              :body artifact})]
        (println "Upload successful."))

      :else
      (throw (ex-info "Error" {})))))

(defn test!
  [bot-config]
  (println "Testing...")
  (let [artifact (->artifact bot-config)
        match-id (:match/id (r/tada! [:api/test-bot!
                                      {:bot-id (:bot/id bot-config)
                                       :digest (artifact/digest artifact)}]))]
    (println "Test successful. View it here: TODO" #_match-id)))

(defn deploy!
  [bot-config]
  (println "Deploying...")
  (let [artifact (->artifact bot-config)]
    (r/tada! [:api/deploy-bot!
              {:bot-id (:bot/id bot-config)
               :digest (artifact/digest artifact)}])
    (println "Deploy successful.")))
