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
   [java.nio.file Files]
   [java.nio.file.attribute FileAttribute]))

;; ssh -nNT -L /tmp/firecracker.socket:/run/firecracker.socket root@192.168.64.2
;; ssh -nNT -L /tmp/v.sock:/home/rafal/v.sock root@192.168.64.2

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

(def BaseContext
  [:map
   [:vm/firecracker-executable-path :string]
   [:vm/firecracker-snapshot-dir-path :string]
   [:vm/firecracker-sock-dir-path :string]
   [:vm/firecracker-timeout-seconds :int]
   [:vm/initramfs-path :string]
   [:vm/sidecar-path :string]
   [:vm/kernel-image-path :string]
   [:vm/vsock-inner-port :int]])

(def VmContext
  [:map
   ;; path to initramfs cpio.gz (rootfs loaded into RAM at boot)
   [:vm/initramfs-path               :string]
   ;; path to img file containing "sidecar" drive
   ;; (containing relay program); becomes /dev/vda (only drive)
   [:vm/sidecar-path                 :string]
   ;; path to uncompressed linux kernel, typically "vmlinux"
   [:vm/kernel-image-path            :string]

   [:vm/firecracker-executable-path  :string]
   [:vm/firecracker-snapshot-dir-path :string]
   [:vm/firecracker-sock-dir-path    :string]
   [:vm/firecracker-executable-path  :string]
   ;; path to where the firecracker control socket
   ;; should be created
   [:vm/firecracker-host-socket-path :string]
   ;; path to where the guest-host messaging socket ("vsock")
   ;; should be created
   [:vm/vsock-host-socket-path       :string]
   ;; port within guest at which vsock will be connected
   [:vm/vsock-inner-port             :int]
   ])

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

(defn configure-and-boot!
  [{:vm/keys [vsock-host-socket-path kernel-image-path initramfs-path sidecar-path] :as vm} fc]
  (tel/event! ::configure-and-boot {:level :info
                                    :data {:vm vm
                                           :fc fc}})
  #_(f/api-request! fc {:method :put
                        :path "/logger"
                        :body {:log_path "firecracker_log.txt"
                               :level "Debug"
                               :show_level true
                               :show_log_origin true}})

  (f/api-request! fc {:method :put
                      :path "/vsock"
                      :body {:guest_cid 3
                             :uds_path vsock-host-socket-path}})

  (f/api-request! fc {:method :put
                      :path "/boot-source"
                      :body {:kernel_image_path kernel-image-path
                             :initrd_path       initramfs-path
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
                             :path_on_host   sidecar-path
                             :is_root_device false
                             :is_read_only   true}})

  (f/api-request! fc {:method :put
                      :path "/machine-config"
                      :body {:vcpu_count 1
                             :mem_size_mib 512}})

  (f/api-request! fc {:method :put
                      :path "/actions"
                      :body {:action_type "InstanceStart"}}))

(defn create-temp-dir!
  [prefix]
  (tel/event! ::create-temp-dir {:level :debug
                                 :prefix prefix})
  (let [path (Files/createTempDirectory prefix (into-array FileAttribute []))]
    (o/with-close-fn
     {:path path}
     (fn [_]
       (tel/event! ::close-temp-dir {:level :debug
                                     :path (str path)})
       (->> (file-seq (.toFile path))
            reverse
            (run! (fn [f] (.delete f))))))))

(defn create-fixed-dir!
  [path-str]
  (let [path (-> (io/file path-str) .toPath)]
    (.mkdirs (.toFile path))
    (o/with-close-fn
     {:path path}
     (fn [_]
       (tel/event! ::close-fixed-dir {:level :debug
                                      :path path-str})
       #_(->> (file-seq path)
            (filter (fn [f]
                      (.isFile f)))
            (map (fn [f]
                   (tel/event! :delete-file {:level :debug
                                             :data {:path f}})))
            (run! (fn [f] (.delete f))))))))

(defn init-from-scratch!
  [vm]
  (let [dir #_(create-temp-dir! "cyber-vm-")
        ;; use fixed dir, because we're going to snapshot
        ;; and until new firecracker version hits, it expects vsock in the same place
        (create-fixed-dir! (:vm/firecracker-sock-dir-path vm))
        vm  (assoc vm
                   :vm/firecracker-host-socket-path (str (:path dir) "/firecracker.sock")
                   :vm/vsock-host-socket-path       (str (:path dir) "/v.sock"))
        fc  (f/init! {:firecracker/socket-path     (:vm/firecracker-host-socket-path vm)
                      :firecracker/log-level (:vm/firecracker-log-level vm)
                      :firecracker/timeout-seconds (:vm/firecracker-timeout-seconds vm)
                      :firecracker/executable-path (:vm/firecracker-executable-path vm)})
        vm (assoc vm :vm/firecracker-instance fc)]
    (tel/event! ::vm-start {:level :debug})
    (Thread/sleep 100)
    (configure-and-boot! vm fc)
    (tel/event! ::vm-wait {:level :debug})
    (Thread/sleep 1500)
    (o/with-close-fn
     vm
     (fn [_]
       (tel/event! ::vm-close {:level :info})
       (o/close fc)
       (.delete (io/file (:vm/vsock-host-socket-path vm)))
       (o/close dir)))))

(defn init-from-snapshot!
  [vm]
  (let [dir (create-fixed-dir! (:vm/firecracker-sock-dir-path vm))
        vm  (assoc vm
                   :vm/firecracker-host-socket-path (str (:path dir) "/firecracker.sock")
                   :vm/vsock-host-socket-path       (str (:path dir) "/v.sock"))
        fc  (f/init! {:firecracker/socket-path     (:vm/firecracker-host-socket-path vm)
                      :firecracker/timeout-seconds (:vm/firecracker-timeout-seconds vm)
                      :firecracker/executable-path (:vm/firecracker-executable-path vm)})
        vm (assoc vm :vm/firecracker-instance fc)]
    (wait-for! (fn [] (check-conn! fc)) 2)
    (tel/event! ::vm-start-snapshot {:level :debug})
    (f/api-request! fc {:method :put
                        :path "/snapshot/load"
                        :body {:snapshot_path (str (:vm/firecracker-snapshot-dir-path vm) "/firecracker.snap")
                               :mem_backend
                               {:backend_path (str (:vm/firecracker-snapshot-dir-path vm) "/firecracker.mem")
                                :backend_type "File"}
                               :resume_vm false
                               ;; useful when next version comes out
                               #_#_:vsock_override {:uds_path vsock-host-socket-path}}})
    (f/api-request! fc {:method :patch
                        :path "/vm"
                        :body {:state "Resumed"}})
    (tel/event! ::vm-wait {:level :debug})
    (o/with-close-fn
     vm
     (fn [_]
       (tel/event! ::vm-close {:level :info})
       (o/close fc)
       (.delete (io/file (:vm/vsock-host-socket-path vm)))
       (o/close dir)))))

(defn eval-unsafe!
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
      (.submit ^Callable (fn [] (eval-unsafe! eval-request)))
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
                          :snapshot_path (str (:vm/firecracker-snapshot-dir-path @*vm) "/firecracker.snap")
                          :mem_file_path (str (:vm/firecracker-snapshot-dir-path @*vm) "/firecracker.mem")}})

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
  (eval-unsafe!
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/home/rafal/hello_musl_x86")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["$ARTIFACT" "Rust"]})

  (eval-unsafe!
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/home/rafal/hello.jar")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["java" "-jar" "$ARTIFACT" "Java"]})

  (eval-unsafe!
   {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/home/rafal/hello.py")
    :eval.request/stdin    (byte-array 0)
    :eval.request/argv     ["python" "$ARTIFACT" "Python"]}))
