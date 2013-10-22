(ns demoasync.main
  (:require [cljs.core.async :refer [<! >! chan put! to-chan timeout ]]
            [demoasync.twitter :refer [tweet-channel]]
            [demoasync.keys :refer [key-chan]]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go-loop alt!]]
    [dommy.macros :refer [node sel sel1]]))

(defn char-span [l]
  [l (node [:span l])])

(defn score-div []
  (let [score (node [:div#score "0"])
        score-chan (chan)]
    (dommy/append! (sel1 :body) score)
    (go-loop [current 0]
       (dommy/set-text! score current)
       (recur (+ current (<! score-chan ))))
    score-chan))

(defn mark-pressed! [span]
   (dommy/add-class! span "pressed"))

(defn update-pos [word pos]
  (dommy/set-style! word :top (str pos "px")))

(defn word-node [char-spans]
  (node [:div.word
          {:style
            {:left (str (* 1000 (js/Math.random)) "px")
             :top "0px"}}
         (map (fn [[_ span]]  span) char-spans)]))


(defprotocol FallProto
  (fall-down! [this])
  (key-pressed! [this k])
  (scoring [this])
  (delete? [this])
  (delete! [this]))

(defrecord FallingWord [text pos delta char-spans word]
  FallProto
  (fall-down! [this]
    (let [new-pos (+ pos delta)]
     (dommy/set-style! word :top (str new-pos "px"))
     (assoc this :pos new-pos)))
  (key-pressed! [this k]
    (let [[char-value span] (first char-spans)]
      (if (= k char-value)
        (do (mark-pressed! span) (update-in this [:char-spans] rest))
        this)))
  (scoring [this]
           (if (empty? char-spans)
             (count text)
             (- (count char-spans))))
  (delete? [this]
           (or (empty? char-spans) (> pos 500)))
  (delete! [this]
           (dommy/remove! word)))

(defn new-falling-word [ctx text]
  (let [char-spans (map char-span text)
        word (word-node char-spans)]
      (dommy/append! ctx word)
      (FallingWord.
         text
         0
         (+ 2 (js/Math.random))
         char-spans
         word)))

(defn word-div [ctx score text]
    (let [[key-channel off] (key-chan)]
      (go-loop [[falling-word timer]
                [(new-falling-word ctx text) (timeout 50)]]
        (if (delete? falling-word)
          (do
            (delete! falling-word)
            (>! score (scoring falling-word))
            (>! off "off"))
          (recur (alt!
             timer ([_] [(fall-down! falling-word) (timeout 50)])
             key-channel ([value] [(key-pressed! falling-word value) timer])))))))

(defn ^:export tweets []
  (let [body (sel1 :body)
        words (tweet-channel "devcon2013")
        score (score-div)]
    (go-loop []
      (<! (timeout 1000))
      (word-div body score (<! words))
      (recur))))
