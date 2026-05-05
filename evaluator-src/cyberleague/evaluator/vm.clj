(ns cyberleague.evaluator.vm
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [cyberleague.evaluator.firecracker :as f]
   [cyberleague.evaluator.open :as o]
   [cyberleague.evaluator.socket :as socket]
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

(def base-context
  {:vm/firecracker-executable-path "/home/rafal/vm/firecracker"
   :vm/root-fs-path                "/home/rafal/vm/out/rootfs.img"
   :vm/sidecar-path                "/home/rafal/vm/out/sidecar.sqsh"
   :vm/kernel-image-path           "/home/rafal/vm/out/vmlinux"
   :vm/vsock-inner-port            52525})

(def VmContext
  [:map
   ;; path to ext4 img file containing the root-fs
   [:vm/root-fs-path                 :string]
   ;; path to img file containing "sidecar" drive
   ;; (containing relay program)
   [:vm/sidecar-path                 :string]
   ;; path to uncompressed linux kernel, typically "vmlinux"
   [:vm/kernel-image-path            :string]

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
  [{:vm/keys [vsock-host-socket-path kernel-image-path root-fs-path sidecar-path] :as vm} fc]
  (tel/event! ::configure-and-boot {:level :info})
  #_(f/api-request! fc {:method :put
                        :path "/logger"
                        :body {:log_path "firecracker_log.txt"
                               :level "Debug"
                               :show_level true
                               :show_log_origin true}})

  (f/api-request! fc {:method :put
                      :path "/cpu-config"
                      :body {:kvm_capabilities ["!56"]
                             :cpuid_modifiers [{:leaf "0x1"
                                                :subleaf "0x0"
                                                :flags 0
                                                :modifiers [{:register "eax"
                                                             :bitmap "0bxxxx000000000011xx00011011110010"}]}]
                             :msr_modifiers [{:addr "0x10a"
                                              :bitmap "0b0000000000000000000000000000000000000000000000000000000000000000"}]}})

  (f/api-request! fc {:method :put
                      :path "/vsock"
                      :body {:guest_cid 3
                             :uds_path vsock-host-socket-path}})

  (f/api-request! fc {:method :put
                      :path "/boot-source"
                      :body {:kernel_image_path kernel-image-path
                             :boot_args
                             (string/join
                              " "
                              #_["console=ttyS0"
                               "panic=1"
                               "pci=off"
                               "root=/dev/vda"
                               "rw"
                               #_"init=/bin/sh"
                               "init=/init.sh"]
                              ;; advanced - prod
                              [;; serial console on ttyS0 at 115200 baud
                               #_"console=ttyS0"
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
                               ;; breaks the VM - leave commented out
                               #_"acpi=off"
                               ;; Firecracker has no HPET timer; skip probing for it
                               "nohpet"
                               ;; skip periodic timer consistency check on boot
                               "no_timer_check"
                               ;; KVM provides a reliable TSC; skip calibration/validation
                               "tsc=reliable"
                               ;; root filesystem
                               "root=/dev/vda"
                               ;; mount root read-write
                               "rw"
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
                               ;; init script
                               "init=/init.sh"])}})

  (f/api-request! fc {:method :put
                      :path "/drives/rootfs"
                      :body {:drive_id       "rootfs"
                             :path_on_host   root-fs-path
                             :is_root_device true
                             :is_read_only   false}})

  (f/api-request! fc {:method :put
                      :path "/drives/sidecar"
                      :body {:drive_id       "sidecar"
                             :path_on_host   sidecar-path
                             :is_root_device false
                             :is_read_only   true}})

  (f/api-request! fc {:method :put
                      :path "/machine-config"
                      :body {:vcpu_count 1
                             :mem_size_mib 128}})

  (f/api-request! fc {:method :put
                      :path "/actions"
                      :body {:action_type "InstanceStart"}}))

(defn boot-and-snapshot!
  [fc]
  ;; WIP
  (comment
    (f/api-request! fc {:method :patch
                        :path "/vm"
                        :body {:state "Paused"}})

    (f/api-request! fc {:method :put
                        :path "/snapshot/create"
                        :body {:snapshot_type "Full"
                               :snapshot_path "/tmp/firecracker.snap"
                               :mem_file_path "/tmp/firecracker.mem"}})

    (f/api-request! fc {:method :put
                        :path "/actions"
                        :body {:action_type "SendCtrlAltDel"}})))

(defn boot-from-snapshot!
  [fc]
  ;; WIP
  (f/api-request! fc {:method :put
                      :path "/snapshot/load"
                      :body {:snapshot_path "/tmp/firecracker.snap"
                             :mem_backend
                             {:backend_path "/tmp/firecracker.mem"
                              :backend_type "File"}
                             :resume_vm false
                             ;; :vsock_override {:uds_path "TODO"}
                             }})
  (f/api-request! fc {:method :patch
                      :path "/vm"
                      :body {:state "Resumed"}}))


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

(defn init!
  [vm]
  (let [dir (create-temp-dir! "cyber-vm-")
        vm  (assoc vm
                   :vm/firecracker-host-socket-path (str (:path dir) "/firecracker.sock")
                   :vm/vsock-host-socket-path       (str (:path dir) "/v.sock"))
        fc  (f/init! {:firecracker/socket-path     (:vm/firecracker-host-socket-path vm)
                      :firecracker/executable-path (:vm/firecracker-executable-path vm)})]
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
       (o/close dir)))))

(defn eval!
  [eval-request]
  ;; vsock's SocketChannel seems to interact with
  ;; http-kits Socket lifecycle management
  ;; must start on a fresh thread or vsock socket is closed early
  @(future (o/with-open+
            [vm (init! base-context)]
            (vsock-request! vm eval-request))))

#_(comment
    (def fc {:firecracker/socket-path "/tmp/firecracker.socket"})

    (check-conn! fc)
    (configure-and-boot-vm! fc)
    (shutdown! fc)

    (eval!
     {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/home/rafal/hello_musl_x86")
      :eval.request/stdin    (byte-array 0)
      :eval.request/argv     ["$ARTIFACT" "Rust"]})

    (eval!
     {:eval.request/artifact (cyberleague.evaluator.artifacts/path->bytes "/Users/rafal/Code/cyberleague-runtime-maker/samples/hello.jar")
      :eval.request/stdin    (byte-array 0)
      :eval.request/argv     ["java" "-jar" "$ARTIFACT" "Java"]}))
