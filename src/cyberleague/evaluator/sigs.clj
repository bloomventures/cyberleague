(ns cyberleague.evaluator.sigs
  (:require
   [buddy.sign.jwt :as jwt]
   [cyberleague.common.config :as config]))

(defn read-token [token]
  (try
    (jwt/unsign token (-> config/config
                          :evaluator
                          :signing-secret))
    (catch Exception _
      nil)))

(defn create-token
  [{:keys [digest]}]
  (jwt/sign {:digest digest
             :exp (-> (java.util.Date.)
                      (.getTime)
                      (quot 1000)
                      (+ 5) ;; s
                      )}
            (-> config/config
                :evaluator
                :signing-secret)))

(defn create-url [{:keys [digest]}]
  (str
   (-> config/config :common :evaluator-url)
   "/upload?token=" (create-token {:digest digest})))

#_(create-token "foo")
#_(verify-token (create-token "foo"))
;; failing:
#_(verify-token "asdasd")
#_(let [t (create-token "foo")]
    (Thread/sleep 6000)
    (verify-token t)
    )
