(ns cyberleague.cli.subcommands.games
  (:require
   [cyberleague.cli.util.remote :as r]))

(defn list-games!
  [_]
  (let [games (r/tada! [:api/games {}])]
    (doseq [game games]
      (println (:game/slug game)))))
