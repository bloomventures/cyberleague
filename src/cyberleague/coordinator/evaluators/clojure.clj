(ns cyberleague.coordinator.evaluators.clojure
  (:require
   [clojure.string :as string]
   [clojure.data.json :as json]
   [sci.core :as sci]
   [cyberleague.coordinator.evaluators.api :as api])
  (:import (java.util.concurrent TimeoutException TimeUnit FutureTask)))

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
                              {:bindings {'println println} :realize-max 10})]
    (json/write-str move)))


;; TODO
(defn test-move
  [function ms]
  (let [task (FutureTask. function)
        thread (Thread. task)]
    (try
      (.start thread)
      (.get task ms TimeUnit/MILLISECONDS)
      (catch TimeoutException e
        (.cancel task true)
        (.stop thread)
        (throw (TimeoutException. "Execution timed out."))))))

#_(try
    (test-move
      (fn []
        (while true
          (Thread/sleep 500)
          (println "1")))
      5000))
