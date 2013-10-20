(ns demoasync.keys
  (:require [cljs.core.async :refer [<! >! chan put! to-chan]]
            [demoasync.ws :refer [ws-chan]]
            [goog.events.EventType]
            [goog.events :as events]
            [demoasync.utils :refer [blocking-pipe]])
  (:require-macros
  [cljs.core.async.macros :as m :refer [go]]
  [dommy.macros :refer [sel1]]))

(defn key-chan []
  (let [k (chan)
        control (chan)
        handler (fn [e] (put! k (.fromCharCode js/String (.-charCode e))))]
    (events/listen (sel1 :body)
       goog.events.EventType.KEYPRESS
       handler)
    (go
     (<! control)
     (events/unlisten (sel1 :body) goog.events.EventType.KEYPRESS handler))
    [k control]))
