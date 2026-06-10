(ns cyberleague.common.config
  (:require
   [bloom.commons.config :as config]))

(def VmConfigBase
  [:map
   [:vm/firecracker-firecracker-executable-path :string]
   [:vm/firecracker-jailer-executable-path :string]
   [:vm/firecracker-jail-base-path :string]
   [:vm/firecracker-jailer-uid :int]
   [:vm/firecracker-jailer-gid :int]
   [:vm/firecracker-snapshot-dir-path :string]
   [:vm/firecracker-timeout-seconds :int]
   [:vm/initramfs-path :string]
   [:vm/sidecar-path :string]
   [:vm/kernel-image-path :string]
   [:vm/vsock-inner-port :int]])

(def config
  (config/read
   "config.edn"
   [:map
    [:common
     [:map
      [:evaluator-url :string]
      [:evaluator-auth-secret :string]
      [:environment [:enum :prod :dev]]]]
    [:evaluator
     [:map
      [:http-port :int]
      [:signing-secret :string]
      [:vm-base-context VmConfigBase]]]
    [:server
     [:map
      [:http-port :int]
      [:coordinator-delay :int]
      [:datomic-uri {:optional true}
       [:re #"^datomic:.*"]]
      [:cookie-secret :string]
      [:github-client-id :string] ;; in :dev, can be garbage
      [:github-client-secret :string] ;; in :dev, can be garbage
      [:github-redirect-uri :string] ;; in :dev, can be garbage
      [:oauth-nonce-secret :string]]]]))

