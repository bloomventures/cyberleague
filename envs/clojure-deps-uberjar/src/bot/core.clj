(ns bot.core
  (:gen-class)
  (:require
   [clojure.data.json :as json]))

(defn run [input]
  {:pong (:ping input)})

(defn -main []
  (-> (slurp *in*)
      (json/read-str :key-fn keyword)
      run
      (json/write-str)
      println))
