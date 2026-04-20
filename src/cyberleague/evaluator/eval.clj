(ns cyberleague.evaluator.eval
  (:require
   [clojure.java.shell :as shell]
   [cyberleague.evaluator.artifacts :as artifacts]
   [cyberleague.evaluator.sci :as sci]))

(defn eval! [digest env-slug input]
  (if (= env-slug "clojure-sci")
    (sci/eval! input
               (artifacts/load digest))
    (throw (ex-info
            "Can't run non-clojure atm"
            {}))))
