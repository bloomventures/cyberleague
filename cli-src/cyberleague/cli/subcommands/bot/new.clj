(ns cyberleague.cli.subcommands.bot.new
  (:require
   [clojure.java.io :as io]
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]
   [cyberleague.cli.util.ednf :as ednf]))

(defn exec!
  [{game-slug :game env-slug :env}]
  (println (f/color :color/yellow "Creating bot..."))
  (let [result (r/tada! [:api/create-bot! {:game-slug game-slug
                                           :env-slug env-slug}])]
    (if result
      (let [{:env/keys [starter-files build-cmd artifact-path run-cmd]}
            (r/tada! [:api/env {:env-slug env-slug
                                :pull-pattern
                                [:env/starter-files
                                 :env/run-cmd
                                 :env/build-cmd
                                 :env/artifact-path]}])]
        (if-let [bot (r/tada! [:api/bot {:bot-id (:bot/id result)}])]
          (let [dir-name (str game-slug "-" (:bot/name bot))
                path (str dir-name "/bot.edn")]
            (io/make-parents path)
            (ednf/write! path
                         {:bot/id      (:bot/id bot)
                          :bot/name    (:bot/name bot)
                          :bot/env     env-slug
                          :bot/game    game-slug
                          :bot/run-cmd run-cmd
                          :bot/build-cmd      build-cmd
                          :bot/build-artifact artifact-path})
            (doseq [[path f-content] starter-files]
              (.mkdirs ^java.io.File (io/file (.getParent ^java.io.File (io/file (str dir-name "/" path)))))
              (spit (str dir-name "/" path) f-content))
            (println "Created" dir-name))
          (println "Error: bot not found")))
      (println "Error: failed to create bot"))))
