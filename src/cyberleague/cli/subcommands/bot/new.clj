(ns cyberleague.cli.subcommands.bot.new
  (:require
   [clojure.java.io :as io]
   [cyberleague.common.envs :as envs]
   [cyberleague.cli.util.remote :as r]
   [cyberleague.cli.util.ednf :as ednf]))

(defn exec!
  [{game-slug :game env-slug :env}]
  (let [result (r/tada! [:api/create-bot! {:game-slug game-slug
                                           :env-slug env-slug}])]
    (if result
      (let [{:env/keys [files build-cmd artifact-path]} (get (envs/all) env-slug)]
        (if-let [bot (r/tada! [:api/bot {:bot-id (:bot/id result)}])]
          (let [dir-name (str game-slug "-" (:bot/name bot))
                path (str dir-name "/bot.edn")]
            (io/make-parents path)
            (ednf/write! path
                         {:bot/id   (:bot/id bot)
                          :bot/name (:bot/name bot)
                          :bot/env  env-slug
                          :bot/game game-slug
                          :bot/build {:bot.build/cmd build-cmd
                                      :bot.build/artifact artifact-path}})
            (doseq [[path f-content] files]
              (spit (str dir-name "/" path)
                    ((eval f-content) nil)))
            (println "Created" dir-name))
          (println "Error: bot not found")))
      (println "Error: failed to create bot"))))
