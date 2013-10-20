(ns demoasync.main
  (:require [cljs.core.async :refer [<! >! chan put! to-chan timeout alts!]]
            [demoasync.twitter :refer [tweet-channel]]
            [demoasync.keys :refer [key-chan]]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go-loop]]
    [dommy.macros :refer [node sel sel1]]))

(defn char-span [l]
  [l (node [:span l])])

(defn score-div []
  (let [score (node [:div#score "0"])
        score-chan (chan)]
    (dommy/append! (sel1 :body) score)
    (go-loop [current 0]
       (dommy/set-text! score current)
       (recur (+ current (<! score-chan )))
    )
    score-chan))

(defn mark-pressed [k char-map]
  (doseq [[_ elem] (filter (fn [[c _]] (= c k)) char-map)]
         (dommy/add-class! elem "pressed")))

(defn update-pos [word pos]
  (dommy/set-style! word :top (str pos "px")))

(defn word-node [char-map]
  (node [:div.word
          {:style
            {:left (str (* 1000 (js/Math.random)) "px")
             :top "0px"}}
         (map (fn [[_ span]]  span) char-map)]))

(defn scoring [char-map text]
  (if (empty? char-map) (count text) (- (count char-map))))

(defn word-div [ctx score text]
  (let [char-map (into {} (map char-span text))
        word (word-node char-map)
        vel (+ 2 (js/Math.random))
        [key-channel off] (key-chan)]
    (dommy/append! ctx word)
    (go-loop [pos 0 timer (timeout 50) char-map char-map]
      (if (or (empty? char-map) (> pos 500))
        (do
          (dommy/remove! word)
          (>! score (scoring char-map text))
          (>! off "off"))
        (let [[value channel] (alts! [timer key-channel])]
            (cond
              (= channel timer)
               (do (update-pos word pos)
                   (recur (+ pos vel) (timeout 50) char-map))
              :default
               (do (mark-pressed value char-map)
                   (recur pos timer (dissoc char-map value)))))))))

(defn ^:export tweets []
  (let [body (sel1 :body)
        words (tweet-channel "codemotion")
        score (score-div)]
    (go-loop []
      (<! (timeout 1000))
      (word-div body score (<! words))
      (recur))))
