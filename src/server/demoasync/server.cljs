(ns demoasync.server
  (:require [cljs.core.async :refer [<! >! chan put! close!]])
  (:require-macros
  [cljs.core.async.macros :as m :refer [ go go-loop]]))


(defn server [port]
  (let [ connect (js/require "connect")]
    (-> (js/connect)
        (.use ((aget connect "static") "public"))
        (.listen port))))

(defn ws [srv f]
      (let [ws-server-ctor
             (.-server (js/require "websocket"))
            ws-server (new ws-server-ctor
                 (clj->js {:httpServer srv}))]
        (.on ws-server "request"
             (fn [request]
               (let [conn (.accept request)
                     in (chan)
                     out (chan)]
                 (.log js/console "Req received")
                 (doto conn
                   (.on "message"
                      (fn [msg]
                        (.log js/console (str "Received " msg))
                        (put! in msg))))
                 (go-loop []
                       (.send conn (<! out))
                       (recur))
                 (f in out))))))



(defn http-channel [options]
  (let [in (chan)
        out (chan)
        https (js/require "https")
        req (.request https (clj->js options)
               (fn [r]
                (.on r "data" (fn [d]
                        (.log js/console (str "Data " d))
                        (put! out d)))
                (.on r "error" (fn [d]
                    (.log js/console "Error" (str d))))
                (.on r "end" (fn [] (close! out)))))]
    (.on req "error" (fn [d] (.log js/console "Client error")))
    (go-loop []
             (let [v (<! in)]
               (case v
                   :end (.end req)
                   (.write req v)))
             (recur))

    [out in]))


(defn get-channel [options]
  (let [[in out] (http-channel (assoc options :method "GET"))]
    (go (>! out :end))
  in))

(defn post-channel [options]
  (http-channel (assoc options :method "POST")))

(defn auth-options [k secret]
  {
    :host "api.twitter.com"
    :path "/oauth2/token"
    :headers {
      :Authorization (str "Basic " (.toString
                             (new js/Buffer (str k ":" secret)) "base64"))
      :Content-Type "application/x-www-form-urlencoded;charset=UTF-8"
   }})

(defn search-channel [query token]
  (let [in (get-channel {:host "api.twitter.com"
                :path (str "/1.1/search/tweets.json?lang=eu&count=50&q=" query )
                :headers {:Authorization (str "Bearer " token)}})]
    (go-loop [buf ""]
      (let [data (<! in)]
         (if data
            (recur (str buf data))
            buf)))))

(defn twitter-key []
  (-> js/process .-env (aget "TWITTER_KEY")))

(defn twitter-secret []
  (-> js/process .-env (aget "TWITTER_SECRET")))

(defn twitter-stream [in out]
  (let [
        [auth-in auth-out]
        (post-channel
         (auth-options (twitter-key) (twitter-secret)))]
        (go
          (>! auth-out "grant_type=client_credentials")
          (>! auth-out :end)
          (let [{tok "access_token"} (js->clj (JSON/parse (str (<! auth-in))))
                tweet-chan ]
            (loop []
              (.log js/console "Sending  ")
               (>! out (<! (search-channel (.-utf8Data (<! in)) tok)))
               (recur))))))


(ws (server 9090) twitter-stream)
