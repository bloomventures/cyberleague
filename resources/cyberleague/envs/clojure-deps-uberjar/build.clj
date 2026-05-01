(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def basis (b/create-basis {:project "deps.edn"}))

(defn uber [_]
  (b/delete {:path "target"})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir "target/classes"})
  (b/uber {:class-dir "target/classes"
           :uber-file "target/bot.jar"
           :basis basis
           :main 'bot.core}))
