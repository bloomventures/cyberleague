(ns cyberleague.coordinator.evaluators.clojure
  (:require
   [clojure.string :as string]
   [clojure.data.json :as json]
   [sci.core :as sci]
   [cyberleague.coordinator.evaluators.api :as api]))

(defmethod api/native-code-runner "clojure"
  [json-state _ code]
  (let [state (json/read-str json-state :key-fn keyword)
        string-to-eval (-> (pr-str
                            '(let [state STATE
                                   bot-function CODE]
                               (bot-function state)))
                           ;; must do this in one pass, or else,
                           ;; potential for code to include 'STATE'
                           (string/replace #"(CODE)|(STATE)"
                                           (fn [[x _ _]]
                                             (case x
                                               "CODE" code
                                               "STATE" (pr-str state)))))
        move (sci/eval-string string-to-eval
                              {:bindings {'println println :realize-max 10}})]
    (json/write-str move)))
