(ns demoasync.main
  (:require [cljs.core.async :refer [<! >! chan put! to-chan timeout ]]
            [demoasync.twitter :refer [tweet-channel]]
            [demoasync.keys :refer [key-chan]]
            [demoasync.falling-word :refer [new-falling-word] :as fw]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go-loop alt!]]
    [dommy.macros :refer [node sel sel1]]))

(defn score-div []
  (let [score (node [:div#score "0"])
        score-chan (chan)]
    (dommy/append! (sel1 :body) score)
    (go-loop [current 0]
       (dommy/set-text! score current)
       (recur (+ current (<! score-chan ))))
    score-chan))

(defn word-div [ctx score text]
    (let [[key-channel off] (key-chan)]
      (go-loop [[falling-word timer]
                [(new-falling-word ctx text) (timeout 50)]]
        (if (fw/delete? falling-word)
          (do
            (fw/delete! falling-word)
            (>! score (fw/scoring falling-word))
            (>! off "off"))
          (recur (alt!
             timer ([_] [(fw/fall-down! falling-word) (timeout 50)])
             key-channel ([value] [(fw/key-pressed! falling-word value) timer])))))))

(defn ^:export tweets []
  (let [body (sel1 :body)
        words (tweet-channel "devcon2013")
        score (score-div)]
    (go-loop []
      (<! (timeout 1000))
      (word-div body score (<! words))
      (recur))))
