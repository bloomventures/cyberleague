(ns cyberleague.client.ui.code-editor
  (:require
   ;; https://github.com/cljsjs/packages/tree/master/codemirror
   [cljsjs.codemirror]
   [cljsjs.codemirror.addon.edit.matchbrackets]
   [cljsjs.codemirror.mode.clojure]
   [reagent.core :as r]
   [zprint.core :as zprint]
   [cyberleague.client.ui.common :as ui]))

(defn reformat [s]
  (zprint/zprint-file-str s "reformat" {:width 80
                                        :style [#_:indent-only :community]}))

(defn code-editor-view
  [{:keys [on-change language value]}]
  (r/with-let
   [code-mirror-instance (atom nil)
    code-mirror-value (atom value)]
   [:div.source {:tw "flex flex-col w-65%"}
    [:div.wrapper {:tw "grow"
                   :ref
                   (fn [el]
                     (when (and el (not @code-mirror-instance))
                       (reset! code-mirror-instance
                               (doto (js/CodeMirror el #js {:value value
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
                                                   (on-change value))))))))}]

    (when (#{"clojure"} language)
      [:div {:tw "bg-#9fa8da p-1"}
       [ui/button {:on-click (fn [_]
                               (->> @code-mirror-instance
                                    .getValue
                                    reformat
                                    (.setValue @code-mirror-instance)))}
        "Reformat"]])]))

;; old code to sync external changes to value prop
#_(fn [this old-args]
    (let [old-value (:value (second old-args))
          new-value (:value (second (r/argv this)))]
      (when (and (not= @code-mirror-value new-value)
                 (not= old-value new-value))
        (.setValue @code-mirror-instance new-value))))
