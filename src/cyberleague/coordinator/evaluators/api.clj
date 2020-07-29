(ns cyberleague.coordinator.evaluators.api)

(defmulti native-code-runner
  ;; inputs
  ;;   language is one of: "clojurescript"
  ;;   code is a string
  ;;   json-state is a json string
  ;; outputs
  ;;   bot's move as a json string
  (fn [json-state language code]
    language))
