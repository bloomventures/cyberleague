(ns cyberleague.client.ui.error-boundary
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [bloom.omni.reagent :as br]
   [reagent.impl.protocols :as reagent.p]))

(defn- render-error-component [{:keys [error info]}]
  [:div
   {:style {:width           "100%"
            :min-width       300
            :backgroundColor "rgba(255,0,0,0.2)"
            :padding         8}}
   [:h6 error]
   [:pre info]])

(def ^:dynamic *render-error* render-error-component)

(defn catch []
  (let [error-state (r/atom nil)
        info-state  (r/atom nil)]
    (r/create-class
     {:component-did-catch
      (fn [_ error info]
        (reset! error-state (let [max-error-length 200]
                              (when-let [err (str error)]
                                (when (-> err count (< max-error-length))
                                  (-> err
                                      (subs 0 max-error-length)
                                      (str " ..."))))))
        (reset! info-state (some->> info
                                    .-componentStack
                                    (string/split-lines)
                                    (remove string/blank?)
                                    (drop-while #(re-find #"re_catch" %))
                                    (take 3)
                                    (string/join "\n"))))
      :reagent-render (fn [& body]
                        (if @error-state
                          [*render-error*
                           {:error @error-state :info @info-state}]
                          (when-not (->> body (remove nil?) empty?)
                            (reagent.p/as-element (br/modded-reagent-compiler) (into [:<>] body)))))})))
