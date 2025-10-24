(ns cyberleague.client.cqrs
  (:require
   [cyberleague.server.cqrs :as cqrs]))

(defmacro events []
  (->> cqrs/events
       (mapv (fn [e] (select-keys e [:id :rest])))))
