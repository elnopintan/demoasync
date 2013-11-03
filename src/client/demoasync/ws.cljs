(ns demoasync.ws
  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros
  [cljs.core.async.macros :as m :refer [go go-loop]]))

(defn ws-chan [uri]
  (let [in (chan)
        out (chan)
        ws (new js/WebSocket uri)]
       (set! (.-onmessage ws)
             (fn [event]
              (put! out (.-data event))))
       (set! (.-onopen ws)
             (fn []
               (go-loop []
                   (.log js/console "In loop")
                   (.send ws (<! in))
                   (recur))))
    [out in]))





