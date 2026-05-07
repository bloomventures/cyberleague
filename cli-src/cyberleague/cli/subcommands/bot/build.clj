(ns cyberleague.cli.subcommands.bot.build
  (:require
   [clojure.java.shell :as sh]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.cli.util.bot-config :as bot-config]
   [cyberleague.cli.util.format :as f]))

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
        (when-let [a (bot-config/->artifact bot-config)]
          (println "Build successful.")
          (println "Digest:" (artifact/digest a)))))
    (println "No build command, skipping.")))

(defn exec!
  [{:keys [dir]}]
  (when-let [bot-config (bot-config/read! dir)]
    (build! bot-config)))
