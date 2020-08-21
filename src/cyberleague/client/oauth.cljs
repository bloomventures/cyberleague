(ns cyberleague.client.oauth)

(defn start-auth-flow! [on-complete]
  (let [width 400
        height 800
        x (/ (- js/window.screen.width width) 2)
        y (/ (- js/window.screen.height height) 2)
        w (.open js/window
                 "/oauth/pre-auth-redirect"
                 "GitHub Auth"
                 (str "width=" width ",height=" height ",screenX=" x ",screenY=" y))
        interval (atom nil)]
    (reset! interval (js/setInterval
                       (fn []
                         (when (.-closed w)
                           (on-complete)
                           (js/clearInterval @interval)))
                       500))))
