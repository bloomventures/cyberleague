(ns cyberleague.coordinator.evaluators.api)

(defmulti native-code-runner
  ;; inputs
  ;;   env-slug is a string
  ;;   code is a string
  ;;   json-state is a json string
  ;; outputs
  ;;   bot's move as a json string
  (fn [json-state env-slug code]
    env-slug))
