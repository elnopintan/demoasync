(ns demoasync.falling-word
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:require-macros
    [dommy.macros :refer [node sel sel1]]))

(defn char-span [l]
  [l (node [:span l])])

(defn word-node [char-spans width]
  (node [:div.word
          {:style
            {:left (str (* width (js/Math.random)) "px")
             :top "0px"}}
         (map (fn [[_ span]]  span) char-spans)]))

(defn mark-pressed! [span]
   (dommy/add-class! span "pressed"))

(defn update-pos [word pos]
  (dommy/set-style! word :top (str pos "px")))

(defprotocol FallProto
  (fall-down! [this])
  (key-pressed! [this k])
  (scoring [this])
  (delete? [this])
  (delete! [this]))

(defrecord FallingWord [text pos delta char-spans height word]
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
           (or (empty? char-spans) (> pos height)))
  (delete! [this]
           (dommy/remove! word)))

(defn new-falling-word [ctx text width height]
  (let [char-spans (map char-span text)
        word (word-node char-spans width)]
      (dommy/append! ctx word)
      (FallingWord.
         text
         0
         (+ 2 (js/Math.random))
         char-spans
         height
         word)))
