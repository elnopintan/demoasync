(ns demoasync.utils
  (:require [cljs.core.async :refer [<! >!]])
  (:require-macros
  [cljs.core.async.macros :as m :refer [go-loop]]))

(defn blocking-pipe [from to]
  (go-loop []
     (let [next-v (<! from)]
       (if next-v
         (do (>! to next-v) (recur))
         nil))))

