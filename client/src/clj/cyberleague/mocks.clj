(ns cyberleague.mocks
  (:require [clojure.string :as string]))

(defn rand-id []
  (string/join "" (take 20 (repeatedly #(rand-int 9)))))

(defn rand-name []
  (string/join "" (take 20 (repeatedly #(rand-nth ["a" "b" "c" "d" "e" "f" "g"])))))

(defn rand-words []
  (string/join " " (take 20 (repeatedly #(rand-nth ["four" "score" "seven" "years" "ago" "our" "founding" "fathers"])))))

(defn bot-name []
  (str (string/join "" (take 3 (repeatedly #(rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))))
       "-"
       (+ 1000 (rand-int 8999))))

(defn gen-code []
  {:id (rand-id)
   :code "(fn [source] )"})

(defn gen-bot [user-id game-id]
  {:id (rand-id)
   :name (bot-name)
   :user-id user-id
   :game-id game-id
   :code-id rand-id
   :code-version ""
   :rating (rand-int 1000)
   :rating-dev (rand-int 200)
   })

(defn gen-user []
  {:id (rand-id)
   :name (rand-name) })

(defn gen-match [bot-ids]
  {:id (rand-id)
   :bots bot-ids
   :moves []
   :first-move (first bot-ids)
   :winner (second bot-ids)})

(defn gen-game []
  {:id (rand-id)
   :name (rand-name)
   :description (rand-words) })
