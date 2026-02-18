(ns cyberleague.coordinator.evaluators.javascript
  (:require
    [cyberleague.coordinator.evaluators.api :as api])
  (:import
    (com.eclipsesource.v8 V8 JavaVoidCallback V8Object V8Array
                          V8RuntimeException Releasable)))

(defn print-handler [out]
  (reify
    com.eclipsesource.v8.JavaVoidCallback
    (^void invoke [this ^V8Object receiver ^V8Array parameters]
      (when (> (.length parameters) 0)
        (let [arg1 (.get parameters 0)]
          (swap! out str arg1)
          ;; docs recommend releasing, but this seems to conflict with the print
          ;; we terminate the runtime soon anyway, so, not releasing should be fine
          #_(when (instance? Releasable)
              (.release (cast Releasable arg1)))))
      nil)))

(defn eval-js [js-string timeout]
  (let [runtime (atom (V8/createV8Runtime))
        out (atom "")
        _ (.registerJavaMethod @runtime (print-handler out) "print")
        _ (future (Thread/sleep timeout)
                  (.terminateExecution @runtime))
        result (try
                 (.executeStringScript @runtime js-string)
                 (catch V8RuntimeException e
                   (throw (ex-info "timeout" {}))))]
    (.release @runtime)
    {:result result
     :out @out}))

(defmethod api/native-code-runner "javascript"
  [json-state _ code]
  (let [string-to-eval (str "const state = JSON.parse(" (pr-str json-state) ");\n"
                            "const bot = " code ";\n"
                            "const result = bot(state);\n"
                            "JSON.stringify(result);")
        {:keys [result out]} (eval-js string-to-eval 500)]
    {:eval/return-value result
     :eval/std-out out}))

#_(api/native-code-runner
    "{ \"hello\": \"world\" }"
    "javascript"
    "function(state) {
       print('this');
       return state.hello.length;
     }")
