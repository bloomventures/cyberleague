(ns cyberleague.coordinator.evaluators.javascript
  (:require
    [cyberleague.coordinator.evaluators.api :as api])
  (:import
    (com.eclipsesource.v8 V8)))

(defn eval-js [js-string]
  (let [runtime (V8/createV8Runtime)
        result (.executeStringScript runtime js-string)]
    (.release runtime)
    result))

(defmethod api/native-code-runner "javascript"
  [json-state _ code]
  (let [string-to-eval (str "const state = JSON.parse(" (pr-str json-state) ");"
                            "const bot = " code ";"
                            "const result = bot(state);"
                            "JSON.stringify(result)")
        result (eval-js string-to-eval)]
    result))

#_(api/native-code-runner
    "{ \"hello\": \"world\" }"
    "javascript"
    "function(state) {
       return state.hello.length;
     }")


