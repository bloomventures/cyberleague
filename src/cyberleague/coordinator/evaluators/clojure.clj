(ns cyberleague.coordinator.evaluators.clojure
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [sci.core :as sci]
   [cyberleague.coordinator.evaluators.api :as api])
  (:import
   (java.io StringWriter)
   (java.util.concurrent FutureTask TimeUnit TimeoutException)))

(defn thread-with-timeout
  "Warning: uses Thread.stop, which is 'unsafe'"
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

(defmethod api/native-code-runner "clojure"
  [json-state _ code]
  (let [state (json/read-str json-state :key-fn keyword)
        form-to-eval (list 'let ['state state
                                 'bot-function (sci/parse-string (sci/init {}) code)]
                           '(bot-function state))
        sw (StringWriter.)
        move (thread-with-timeout
              (fn [] (sci/binding
                      [sci/out sw]
                       (sci/eval-form
                        (sci/init {:realize-max 10})
                        form-to-eval)))
              500)]
    {:eval/return-value (json/write-str move)
     :eval/std-out (str sw)}))

#_(try
    (test-move
     (fn []
       (while true
         (Thread/sleep 500)
         (println "1")))
     5000))

(comment
  (api/native-code-runner
   "{\"the-state\": 12345}"
   "clojure"
   "(fn [x] (get x :the-state))"))
