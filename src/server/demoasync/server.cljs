(ns demoasync.server
  (:require [cljs.core.async :refer [<! >! chan put!]])
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
                      (fn [msg] (put! in msg))))
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
                        (.log js/console (str d))
                        (put! out d)))
                (.on r "error" (fn [d]
                    (.log js/console "Error" (str d))))))]
    (go-loop []
             (let [v (<! in)]
               (case v
                   :end (.end req)
                   (.write req v)))
             (recur))

    [in out]))


(defn get-channel [options]
  (let [[in out] (http-channel (assoc options :method "GET"))]
    (go (>! in :end))
  out))

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
  (get-channel {:host "api.twitter.com"
                :path (str "/1.1/search/tweets.json?q=" query )
                :headers {:Authorization (str "Bearer " token)}}))

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
          (.log js/console (str "Key " (twitter-key)))
          (>! auth-in "grant_type=client_credentials")
          (>! auth-in :end)
          (let [{tok "access_token"} (js->clj (JSON/parse (str (<! auth-out ))))
                tweet-chan (search-channel "clojure" tok)]
            (loop []
              (>! out (str (<! tweet-chan)))
              (<! in)
              (recur))))))


(ws (server 8080) twitter-stream)
