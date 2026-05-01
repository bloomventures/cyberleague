(ns cyberleague.evaluator.eval
  (:require
   [clojure.string :as string]
   [cyberleague.evaluator.artifacts :as artifacts]
   [cyberleague.evaluator.sci :as sci]
   [cyberleague.evaluator.vm :as vm]
   [cyberleague.common.envs :as envs]
   [taoensso.telemere :as tel]))

(defn vm-eval!
  [{:keys [input digest env-slug]}]
  (let [result (vm/eval!
                {:eval.request/artifact (artifacts/load-bytes digest)
                 :eval.request/stdin    (.getBytes input "UTF-8")
                 :eval.request/argv     (-> (envs/by-slug env-slug)
                                            :env/argv)})]
    {:eval/return-value (string/trim (:eval.response/stdout result))
     :eval/std-out ""}))

#_(vm-eval! {:input "{}"
             :digest "9aa91c49c58ee8d7d1e5827034c32c323bc38c4fdba742082488c39cbbb5bc52"
             :env-slug "rust-musl"})

#_(vm-eval! {:input "{\"current-trophy\": 12}"
             :digest "19ba2915546340729782be7c346ec21f83f694a08026d0a8c6dfab49a5ff4a3f"
             :env-slug "rust-musl"})

(defn eval!
  [{:keys [input digest env-slug]}]
  (tel/trace!
   {:id ::eval
    :level :info
    :data {:digest digest
           :env-slug env-slug
           :input input}}
   (case (:env/runtime (envs/by-slug env-slug))
     :runtime/sci
     (sci/eval! input
                (artifacts/load-string digest))
     :runtime/firecracker
     (vm-eval! {:input input
                :env-slug env-slug
                :digest digest})
     ;; else
     (throw (ex-info
             (str "Unknown/unsupported env:" env-slug)
             {})))))

#_(eval!
   {:digest "19ba2915546340729782be7c346ec21f83f694a08026d0a8c6dfab49a5ff4a3f"
    :env-slug "rust-musl"
    :input "{\"player-cards\":{\"opponent\":[7,1,4,13,6,3,12,2,11,9,5,10,8],\"me\":[7,1,4,13,6,3,12,2,11,9,5,10,8]},\"trophy-cards\":[7,1,4,13,6,3,12,2,11,9,10,8],\"current-trophy\":5,\"history\":[]}"})
