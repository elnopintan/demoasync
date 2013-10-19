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
              (put! in (.-data event))))
       (set! (.-onopen ws)
             (fn []
               (go-loop []
                   (.send ws (<! out))
                   (recur))))
    [in out]))





