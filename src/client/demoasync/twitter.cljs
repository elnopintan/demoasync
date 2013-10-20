(ns demoasync.twitter
  (:require [cljs.core.async :refer [<! >! chan put! to-chan]]
            [demoasync.ws :refer [ws-chan]]
            [demoasync.utils :refer [blocking-pipe]])
  (:require-macros
  [cljs.core.async.macros :as m :refer [go-loop]]))

(defn tweet-seq [text]
  (for [ status (-> text JSON/parse js->clj (get "statuses"))
         word (-> (status "text") (.split " "))]
    word))

(defn tweet-channel [search]
  (let [out (chan)
        [ws-in ws-out] (ws-chan  "ws://localhost:8080")]
    (go-loop []
          (>! ws-out search)
          (.log js/console "Receiving")
          (let [words (to-chan (tweet-seq (<! ws-in)))]
            (<! (blocking-pipe words out)))
          (recur))
    out))

