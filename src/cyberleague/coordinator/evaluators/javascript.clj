(ns cyberleague.coordinator.evaluators.javascript
  (:require
    [cyberleague.coordinator.evaluators.api :as api])
  (:import
    (com.eclipsesource.v8 V8 JavaVoidCallback V8Object V8Array
                          V8RuntimeException Releasable)))

(def print-handler
  (reify
    com.eclipsesource.v8.JavaVoidCallback
    (^void invoke [this ^V8Object receiver ^V8Array parameters]
      (when (> (.length parameters) 0)
        (let [arg1 (.get parameters 0)]
          (println arg1)
          ;; docs recommend releasing, but this seems to conflict with the print
          ;; we terminate the runtime soon anyway, so, not releasing should be fine
          #_(when (instance? Releasable)
              (.release (cast Releasable arg1)))))
      nil)))

(defonce error (atom nil))

(defn eval-js [js-string timeout]
  (let [runtime (atom (V8/createV8Runtime))
        _ (.registerJavaMethod @runtime print-handler "print")
        _ (future (Thread/sleep timeout)
                  (.terminateExecution @runtime))
        result (try
                 (.executeStringScript @runtime js-string)
                 (catch V8RuntimeException e
                   (throw (ex-info "timeout" {}))))]
    (.release @runtime)
    result))

(defmethod api/native-code-runner "javascript"
  [json-state _ code]
  (let [string-to-eval (str "const state = JSON.parse(" (pr-str json-state) ");\n"
                            "const bot = " code ";\n"
                            "const result = bot(state);\n"
                            "JSON.stringify(result);")
        result (eval-js string-to-eval 500)]
    result))

#_(api/native-code-runner
    "{ \"hello\": \"world\" }"
    "javascript"
    "function(state) {
       return state.hello.length;
     }")
