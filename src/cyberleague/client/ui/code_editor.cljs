(ns cyberleague.client.ui.code-editor
  (:require
    [reagent.core :as r]
    ;; https://github.com/cljsjs/packages/tree/master/codemirror
    [cljsjs.codemirror]
    [cljsjs.codemirror.mode.clojure]
    [cljsjs.codemirror.addon.edit.matchbrackets]
    [zprint.core :as zprint]))

(defn reformat [s]
  (zprint/zprint-file-str s "reformat" {:width 80
                                        :style [#_:indent-only :community]}))

(defn code-editor-view [{:keys [on-change language value]}]
  (let [element (atom nil)
        code-mirror-instance (atom nil)
        code-mirror-value (atom value)]
    (r/create-class
      {:component-did-mount
       (fn []
         (reset! code-mirror-instance
          (doto (js/CodeMirror @element #js {:value value
                                             :mode (case language
                                                     "clojure" "clojure"
                                                     "javascript" "javascript")
                                             :matchBrackets true
                                             ;; disable for now, b/c it breaks on
                                             ;; reagent re-render
                                             #_#_:lineNumbers true})
            (.on "change" (fn [editor]
                            (let [value (.getValue editor)]
                              (reset! code-mirror-value value)
                              (on-change value)))))))
       :reagent-render
       (fn [_]
         [:div.source
          (when (#{"clojure"} language)
              [:button {:on-click (fn [_]
                                    (->> @code-mirror-instance
                                         .getValue
                                         reformat
                                         (.setValue @code-mirror-instance)))}
               "Reformat"])
          [:div.wrapper {:ref (fn [el] (reset! element el))}]])
       :component-did-update
       (fn [this old-args]
         (let [old-value (:value (second old-args))
               new-value (:value (second (r/argv this)))]
           (when (and (not= @code-mirror-value new-value)
                      (not= old-value new-value))
             (.setValue @code-mirror-instance new-value))))})))
