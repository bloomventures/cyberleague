(ns cyberleague.evaluator.wasm
  (:import
   (com.dylibso.chicory.runtime Store)
   (com.dylibso.chicory.wasi WasiExitException WasiOptions WasiPreview1)
   (com.dylibso.chicory.wasm Parser)
   (java.io ByteArrayInputStream ByteArrayOutputStream)))

(defn eval!
  [{:eval.request/keys [artifact stdin]}]
  (let [stdout    (ByteArrayOutputStream.)
        stderr    (ByteArrayOutputStream.)
        wasi-opts (-> (WasiOptions/builder)
                      (.withStdin (ByteArrayInputStream. stdin))
                      (.withStdout stdout)
                      (.withStderr stderr)
                      .build)
        wasi      (-> (WasiPreview1/builder)
                      (.withOptions wasi-opts)
                      .build)
        store     (-> (Store.)
                      (.addFunction (.toHostFunctions wasi)))
        module    (Parser/parse ^bytes artifact)
        exit-code (try
                    (.instantiate store "wasm-bot" module)
                    0
                    (catch WasiExitException e
                      (.exitCode e))
                    (catch Exception _
                      1))]
    {:eval.response/exit   exit-code
     :eval.response/stdout (.toString stdout)
     :eval.response/stderr (.toString stderr)}))

(comment
  (eval! {:eval.request/artifact
          (java.nio.file.Files/readAllBytes
           (java.nio.file.Path/of "goofspiel-wasip1.wasm" (into-array String [])))
          :eval.request/stdin
          (.getBytes "{\"ping\": 123}")}))
