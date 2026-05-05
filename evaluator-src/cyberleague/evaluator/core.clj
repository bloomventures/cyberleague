(ns cyberleague.evaluator.core
  "The 'evaluator' exposes an http API to run executables in various environments"
  (:require
   [clojure.java.io :as io]
   [bloom.commons.crypto :as crypto]
   [org.httpkit.server :as http]
   [muuntaja.middleware :as mj]
   [ring.middleware.defaults :as rmd]
   [tada.events.core :as tada]
   [tada.events.ring :as tada.ring]
   [taoensso.telemere :as tel]
   [cyberleague.evaluator.artifacts :as artifacts]
   [cyberleague.evaluator.eval :as eval]
   [cyberleague.common.config :as config]
   [cyberleague.common.envs :as envs]
   [cyberleague.common.artifact :as artifact]
   [cyberleague.common.schema :as s]
   [cyberleague.evaluator.sigs :as sigs]))

(defn allowed? [secret]
  (crypto/slow= secret (-> config/config
                           :common
                           :evaluator-auth-secret)))

(defonce t (tada/init :malli))

(def commands
  [{:id :evaluator/prepare!
    :rest [:post "/prepare"]
    :params {:auth-secret :string
             :digest s/Digest}
    :conditions
    (fn [{:keys [auth-secret]}]
      [[#(allowed? auth-secret)]])
    :return
    (fn [{:keys [digest]}]
      (if (artifacts/exists? digest)
        {:skip? true}
        {:upload-url (sigs/create-url {:digest digest})}))}

   ;; this route is directly accessed by CLI
   {:id :evaluator/upload!
    :rest [:post "/upload"]
    :params {:token :string
             :file :any ;; file
             }
    :conditions
    (fn [{:keys [token file]}]
      (let [{:keys [digest]} (sigs/read-token token)]
        [[#(not (artifacts/exists? digest))
          "Artifact already exists"]
         [#(= (artifact/digest (io/input-stream file)) digest)
          "Digest does not match artifact"]]))
    :effect
    (fn [{:keys [file]}]
      (artifacts/store! file))}

   {:id :evaluator/run!
    :rest [:post "/run"]
    :params {:auth-secret :string
             :digest :string
             :env-slug :string
             :input :string}
    :conditions
    (fn [{:keys [auth-secret digest env-slug _input]}]
      [[#(allowed? auth-secret)]
       [#(envs/enabled? env-slug) "Unknown env"]
       [#(artifacts/exists? digest) "An artifact with this digest does not exist."]])
    :effect
    (fn [{:keys [digest env-slug input]}]
      (eval/eval! {:input input
                   :digest digest
                   :env-slug env-slug}))
    :return :tada/effect-return}])

(tada/register! t commands)

#_(tada/do! t :evaluator/prepare! {:digest "a6213fe8321a5ce0e65e3560ba0eda5d169352dbd2e02e653b10d109eeff56d0"})

(def rest->event-id
  (zipmap (map :rest commands)
          (map :id commands)))

(defn handler [request]
  (when-let [event-id (rest->event-id
                       [(:request-method request)
                        (:uri request)])]
    (tada.ring/ring-dispatch-event!
     t
     event-id
     (-> (:params request)
         (merge (:body-params request))
         (merge (when (= (:content-type request)
                         "application/octet-stream")
                  {:file (.readAllBytes (:body request))}))))))

#_(handler {:request-method :post
            :uri "/prepare"
            :body-params {:digest "xays"}})

(def app
  (-> handler
      mj/wrap-format
      (rmd/wrap-defaults rmd/api-defaults)))

(defonce server
  (atom nil))

(defn start! []
  (let [port (-> config/config :evaluator :http-port)]
    (tel/event! ::start-server {:level :info
                                :port port})
    (reset! server (http/run-server #'app {:port port}))))

(defn stop! []
  (when @server
    (@server)))

#_(start!)


