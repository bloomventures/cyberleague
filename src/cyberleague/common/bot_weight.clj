(ns cyberleague.common.bot-weight
  (:import
   (com.aayushatharva.brotli4j Brotli4jLoader)
   (com.aayushatharva.brotli4j.encoder Encoder Encoder$Parameters)))

(Brotli4jLoader/ensureAvailability)

(defn code-weight [code]
  (let [params (doto (Encoder$Parameters.) (.setQuality 11))]
    (-> (.getBytes code "UTF-8")
        (Encoder/compress params)
        alength)))

#_(code-weight "(* (+ 1 2 3 4) (+ 1 2 3 4))")
#_(code-weight "(+ 1 2 3 4)")

