(ns cyberleague.client.ui.code-editor
  (:require
    [reagent.core :as r]
    ;; https://github.com/cljsjs/packages/tree/master/codemirror
    [cljsjs.codemirror]
    [cljsjs.codemirror.mode.clojure]
    [cljsjs.codemirror.addon.edit.matchbrackets]))

(defn code-editor-view [{:keys [on-change language value]}]
  (let [element (atom nil)]
    (r/create-class
      {:component-did-mount
       (fn []
         (doto (js/CodeMirror @element #js {:value value
                                            :mode (case language
                                                    "clojurescript" "clojure"
                                                    "javascript" "javascript")
                                            :matchBrackets true
                                            ;; disable for now, b/c it breaks on
                                            ;; reagent re-render
                                            #_#_:lineNumbers true})
           (.on "change" (fn [editor]
                           (on-change (.getValue editor))))))
       :reagent-render
       (fn [_]
         [:div.source {:ref (fn [el] (reset! element el))}])})))
