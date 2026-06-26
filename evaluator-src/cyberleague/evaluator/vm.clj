(ns cyberleague.evaluator.vm
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [cyberleague.evaluator.firecracker :as f]
   [cyberleague.evaluator.open :as o]
   [cyberleague.evaluator.socket :as socket]
   [cyberleague.common.config :as config]
   [taoensso.telemere :as tel])
  (:import
   [java.nio.file Files]))

;; firecracker
;;   resources:
;;     main repo
;;       https://github.com/firecracker-microvm/firecracker
;;     demo repo
;;       https://github.com/firecracker-microvm/firecracker-demo
;;     swagger api:
;;       https://github.com/firecracker-microvm/firecracker/blob/main/src/firecracker/swagger/firecracker.yaml
;;     blogs
;;       https://jacquesverre.com/blog/gentle-introduction-to-firecracker
;;       https://jvns.ca/blog/2021/01/23/firecracker--start-a-vm-in-less-than-a-second/

(def VmContext
  [:map
   ;; path to initramfs.cpio.gz
   ;; (rootfs loaded into RAM at boot)
   [:vm/initramfs-path :string]
   ;; path to img file containing "sidecar" drive
   ;; (containing relay program); becomes /dev/vda (only drive)
   [:vm/sidecar-path :string]
   ;; path to uncompressed linux kernel, typically "vmlinux"
   [:vm/kernel-image-path :string]
   ;; path to where the firecracker control socket
   ;; should be created
   [:vm/firecracker-firecracker-executable-path :string]
   [:vm/firecracker-jailer-executable-path :string]
   [:vm/firecracker-jailer-uid :int]
   [:vm/firecracker-jailer-gid :int]
   [:vm/firecracker-snapshot-dir-path :string]
   [:vm/firecracker-timeout-seconds :int]
   ;; path to where the guest-host messaging socket ("vsock")
   ;; should be created
   [:vm/vsock-host-socket-path :string]
   ;; port within guest at which vsock will be connected
   [:vm/vsock-inner-port :int]])

(defn- wait-for!
  "Calls f repeatedly until it returns without throwing."
  [f poll-ms]
  (loop []
    (when (= ::retry
             (try (f) nil
                  (catch Exception _ ::retry)))
      (Thread/sleep poll-ms)
      (recur))))

(defn vsock-request!
  [vm eval-request]
  (tel/event! ::vsock-request {:level :debug
                               :eval-request eval-request})
  (socket/vsock-request!
   {:vsock/host-socket-path (:vm/vsock-host-socket-path vm)
    :vsock/guest-port       (:vm/vsock-inner-port vm)}
   eval-request))

(defn check-conn! [fc]
  (f/api-request! fc {:method :get :path "/"}))

(defn shutdown! [fc]
  (f/api-request! fc {:method :put
                      :path "/actions"
                      :body {:action_type "SendCtrlAltDel"}}))

(defn configure-and-boot! [fc]
  (tel/event! ::configure-and-boot {:level :info
                                    :data {:fc fc}})
  #_(f/api-request! fc {:method :put
                        :path "/logger"
                        :body {:log_path "/firecracker_log.txt"
                               :level "Debug"
                               :show_level true
                               :show_log_origin true}})

  (f/api-request! fc {:method :put
                      :path "/vsock"
                      :body {:guest_cid 3
                             :uds_path "/v.sock"}})

  (f/api-request! fc {:method :put
                      :path "/boot-source"
                      :body {:kernel_image_path "/vmlinux"
                             :initrd_path       "/initrd.gz"
                             :boot_args
                             (string/join
                              " "
                              [;; serial console on ttyS0 at 115200 baud
                               "console=ttyS0,115200n8"
                               ;; limit serial port probing to just ttyS0 (default scans 4)
                               "8250.nr_uarts=1"
                               ;; suppress most kernel messages to console
                               "quiet"
                               ;; only show critical kernel log messages
                               "loglevel=0"
                               ;; reboot via keyboard controller
                               ;; (only option in KVM without ACPI)
                               "reboot=k"
                               ;; halt immediately on kernel panic
                               "panic=1"
                               ;; disable PCI bus enumeration (Firecracker has no PCI)
                               "pci=off"
                               ;; Firecracker has no HPET timer; skip probing for it
                               "nohpet"
                               ;; skip periodic timer consistency check on boot
                               "no_timer_check"
                               ;; KVM provides a reliable TSC; skip calibration/validation
                               "tsc=reliable"
                               ;; use CPU hardware RNG for entropy; avoids stalling
                               ;; waiting for entropy pool to fill (biggest boot-time win)
                               "random.trust_cpu=on"
                               ;; disable Spectre/Meltdown mitigations
                               ;; (safe for single-tenant VMs)
                               "mitigations=off"
                               ;; skip ASLR randomization pass (faster + deterministic addresses)
                               "nokaslr"
                               ;; skip RAID autodetection at boot
                               "raid=noautodetect"
                               ;; initramfs init entry point
                               "rdinit=/init"])}})

  ;; sidecar is the only drive; becomes /dev/vda inside the guest
  (f/api-request! fc {:method :put
                      :path "/drives/sidecar"
                      :body {:drive_id       "sidecar"
                             :path_on_host   "/sidecar.img"
                             :is_root_device false
                             :is_read_only   true}})

  (f/api-request! fc {:method :put
                      :path "/machine-config"
                      :body {:vcpu_count 1
                             :mem_size_mib 512}})

  (f/api-request! fc {:method :put
                      :path "/actions"
                      :body {:action_type "InstanceStart"}}))

(defn link-into-jail!
  [jail-root host-path jail-filename]
  (let [dest (io/file jail-root jail-filename)]
    (.mkdirs (.getParentFile dest))
    (Files/createLink (.toPath dest) (.toPath (io/file host-path)))))

(defn firecracker-context [vm]
  {:firecracker/firecracker-executable-path  (:vm/firecracker-firecracker-executable-path vm)
   :firecracker/jail-base-path               (:vm/firecracker-jail-base-path vm)
   :firecracker/jailer-executable-path       (:vm/firecracker-jailer-executable-path vm)
   :firecracker/jailer-uid                   (:vm/firecracker-jailer-uid vm)
   :firecracker/jailer-gid                   (:vm/firecracker-jailer-gid vm)
   :firecracker/timeout-seconds              (:vm/firecracker-timeout-seconds vm)
   :firecracker/log-level                    (:vm/firecracker-log-level vm)})

(defn acquire!
  "Like o/with-open+, but the acquired resources ESCAPE the call instead of
  being closed at the end of a scope.

  Calls (build track!), where track! registers a closeable (optionally with a
  hint) and returns it. On success, returns build's result carrying a close fn
  (see o/with-close-fn) that closes everything acquired, in reverse order, via
  open's close-all. If build throws, the acquired resources are closed
  immediately and the original exception re-thrown, with any close exceptions
  added as suppressed.

  Use when a resource handle must outlive the function that opens it — e.g.
  returned to a caller who later closes it with o/with-open+."
  [build]
  (let [;; using volatile, because that's what with-open+ does
        acquired (volatile! ())
        track!   (fn
                   ([closeable] (vswap! acquired conj [closeable nil]) closeable)
                   ([closeable hint] (vswap! acquired conj [closeable hint]) closeable))]
    (try
      (o/with-close-fn
       (build track!)
       (fn [_]
         (when-let [t (#'o/close-all @acquired nil)]
           (throw t))))
      (catch Throwable t
        (throw (#'o/close-all @acquired t))))))

(defn init-from-scratch!
  [vm]
  (acquire!
   (fn [track!]
     (let [jail      (track! (f/prepare-jail! (firecracker-context vm)) ::jail)
           jail-root (:firecracker/jail-root jail)]
       (link-into-jail! jail-root (:vm/kernel-image-path vm) "vmlinux")
       (link-into-jail! jail-root (:vm/initramfs-path vm) "initrd.gz")
       (link-into-jail! jail-root (:vm/sidecar-path vm) "sidecar.img")
       (let [fc (track! (f/start! jail) ::firecracker)]
         (tel/event! ::vm-start {:level :debug})
         (Thread/sleep 5000)
         (configure-and-boot! fc)
         (tel/event! ::vm-wait {:level :debug})
         (Thread/sleep 1500)
         (assoc vm
                :vm/vsock-host-socket-path (str jail-root "/v.sock")
                :vm/firecracker-instance fc))))))

(defn init-from-snapshot!
  [vm]
  (acquire!
   (fn [track!]
     (let [jail         (track! (f/prepare-jail! (firecracker-context vm)) ::jail)
           jail-root    (:firecracker/jail-root jail)
           snapshot-dir (:vm/firecracker-snapshot-dir-path vm)]
       (link-into-jail! jail-root (:vm/kernel-image-path vm) "vmlinux")
       (link-into-jail! jail-root (:vm/initramfs-path vm) "initrd.gz")
       (link-into-jail! jail-root (:vm/sidecar-path vm) "sidecar.img")
       (link-into-jail! jail-root (str snapshot-dir "/firecracker.snap") "firecracker.snap")
       (link-into-jail! jail-root (str snapshot-dir "/firecracker.mem") "firecracker.mem")
       (let [fc (track! (f/start! jail) ::firecracker)]
         (wait-for! (fn [] (check-conn! fc)) 2)
         (tel/event! ::vm-start-snapshot {:level :debug})
         (f/api-request! fc {:method :put
                             :path "/snapshot/load"
                             :body {:snapshot_path "/firecracker.snap"
                                    :mem_backend
                                    {:backend_path "/firecracker.mem"
                                     :backend_type "File"}
                                    :resume_vm false
                                    #_#_:vsock_override {:uds_path "/v.sock"}}})
         (f/api-request! fc {:method :patch
                             :path "/vm"
                             :body {:state "Resumed"}})
         (tel/event! ::vm-wait {:level :debug})
         (assoc vm
                :vm/vsock-host-socket-path (str jail-root "/v.sock")
                :vm/firecracker-instance fc))))))

(defn eval-from-scratch!
  [eval-request]
  @(future (o/with-open+
            [vm (init-from-scratch! (-> config/config :evaluator :vm-base-context))]
            (vsock-request! vm eval-request))))

(defn eval-from-snapshot!
  [eval-request]
  ;; vsock's SocketChannel seems to interact with
  ;; http-kits Socket lifecycle management
  ;; must start on a fresh thread or vsock socket is closed early
  @(future (o/with-open+
            [vm (init-from-snapshot! (-> config/config :evaluator :vm-base-context))]
            (vsock-request! vm eval-request))))

;; serialize all eval requests into a queue
(defonce ^:private eval-executor
  (java.util.concurrent.ThreadPoolExecutor.
   1 1 0 java.util.concurrent.TimeUnit/MILLISECONDS
   (java.util.concurrent.LinkedBlockingQueue.)))

(defn status []
  {:running? (not (.isShutdown eval-executor))
   :queued   (.. eval-executor getQueue size)})

(defn eval!
  [eval-request]
  (-> eval-executor
      (.submit ^Callable (fn [] (eval-from-snapshot! eval-request)))
      .get))

(comment
  ;; create snapshot
  (def *vm (atom nil))

  (reset! *vm (init-from-scratch!
               (assoc (-> config/config :evaluator :vm-base-context)
                      :vm/firecracker-timeout-seconds 100000)))

  (f/api-request! (:vm/firecracker-instance @*vm)
                  {:method :patch
                   :path "/vm"
                   :body {:state "Paused"}})

  (f/api-request! (:vm/firecracker-instance @*vm)
                  {:method :put
                   :path "/snapshot/create"
                   :body {:snapshot_type "Full"
                          :snapshot_path (str #_(:vm/firecracker-snapshot-dir-path @*vm) "/firecracker.snap")
                          :mem_file_path (str #_(:vm/firecracker-snapshot-dir-path @*vm) "/firecracker.mem")}})

  ;; TODO
  ;; copy snap and mem out of the dir

  (f/api-request! (:vm/firecracker-instance @*vm)
                  {:method :patch
                   :path "/vm"
                   :body {:state "Resumed"}})

  (f/api-request! (:vm/firecracker-instance @*vm)
                  {:method :put
                   :path "/actions"
                   :body {:action_type "SendCtrlAltDel"}}))

(comment

  (def *vm (atom nil))

  (reset! *vm (init-from-scratch!
               (assoc (-> config/config :evaluator :vm-base-context)
                      :vm/firecracker-timeout-seconds 100000)))

  (f/api-request! (:vm/firecracker-instance @*vm) {:method :get :path "/"})

  (vsock-request!
   @*vm
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/home/rafal/hello_musl_x86")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["$ARTIFACT" "Rust"]})

  (vsock-request!
   @*vm
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/home/rafal/hello.jar")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["java" "-jar" "$ARTIFACT" "Java"]})

  (vsock-request!
   @*vm
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/home/rafal/hello.py")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["python" "$ARTIFACT" "Python"]}))

(comment
  (eval-from-snapshot!
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/tmp/hello_musl_x86")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["$ARTIFACT" "Rust"]})

  (eval-from-snapshot!
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/tmp/hello.jar")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["java" "-jar" "$ARTIFACT" "Java"]})

  (eval-from-snapshot!
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/tmp/hello.py")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["python" "$ARTIFACT" "Python"]}))
