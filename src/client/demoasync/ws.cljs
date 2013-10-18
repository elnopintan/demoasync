(ns demoasync.ws
  (:require [cljs.core.async :refer [<! >! chan put!]])
  (:require-macros
  [cljs.core.async.macros :as m :refer [go go-loop]]))


(defn twitter-chan []
  (let [in (chan)
        ws (new js/WebSocket "ws://localhost:8080")]
       (set! (.-onmessage ws)
             (fn [event] (go (>! in (.-data event))
                             (.send ws "next"))))
    in))


(defn ^:export tweets []
  (let [ws-in (twitter-chan)]
    (go-loop [] (js/alert (<! ws-in))(recur))))

