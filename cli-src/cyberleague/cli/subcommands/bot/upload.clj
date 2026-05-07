(ns cyberleague.cli.subcommands.bot.upload
  (:require
   [clojure.java.io :as io]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.common.transit-client :as http]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]
   [cyberleague.cli.util.weight :as weight]))

(defn upload!
  [bot-config]
  (println (f/color :color/yellow "Uploading..."))
  (let [a (bot-config/->artifact bot-config)
        ;; by having this in the CLI,
        ;; we are effectively letting bots "self report" their weight
        ;; (in theory, one could hit this endpoint directly)
        ;; we don't want to use build file weights b/c it punishes
        ;; langs that have to statically link
        ;; for now, this is fine; one day maybe we'll do the builds
        ;; on our own servers
        w (weight/dir-weight (io/file (bot-config/dir bot-config)))
        _ (println "Digest:" (artifact/digest a))
        _ (println "Weight:" w)
        response (r/tada! [:api/artifact-upload-prepare!
                           {:bot-id (:bot/id bot-config)
                            :env-slug (:bot/env bot-config)
                            :digest (artifact/digest a)
                            :weight w}])]
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
              :body a})
            (println "Upload successful.")
            (catch Exception _
              (println "Upload error.")))))

      :else
      (throw (ex-info "Error uploading" {})))))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (upload! bot-config)))
