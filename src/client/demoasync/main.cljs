(ns demoasync.main
  (:require [cljs.core.async :refer [<! >! chan put! to-chan timeout]]
            [demoasync.twitter :refer [tweet-channel]]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go-loop]]
    [dommy.macros :refer [node sel sel1]]))

(defn ^:export tweets []
  (let [body (sel1 :body)
        words (tweet-channel)]
    (go-loop []
      (<! (timeout 1000))
      (dommy/append! body [:div#word (<! words)])
      (recur))))
