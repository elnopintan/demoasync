(ns demoasync.twitter
  (:require [cljs.core.async :refer [<! >! chan put! to-chan timeout]]
            [demoasync.ws :refer [ws-chan]]
            [demoasync.utils :refer [blocking-pipe]])
  (:require-macros
  [cljs.core.async.macros :as m :refer [go-loop alt!]]))

(defn tweet-seq [text]
  (for [ status (-> text JSON/parse js->clj (get "statuses"))
         word (-> (status "text") (.split " "))
         :when (re-matches #"\w+" word)]
    word))

(defn plan-b []
  (let [quijote "En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no ha mucho tiempo que vivía un hidalgo de los de lanza en astillero, adarga antigua, rocín flaco y galgo corredor. Una olla de algo más vaca que carnero, salpicón las más noches, duelos y quebrantos los sábados, lantejas los viernes, algún palomino de añadidura los domingos, consumían las tres partes de su hacienda. El resto della concluían sayo de velarte, calzas de velludo para las fiestas, con sus pantuflos de lo mesmo, y los días de entresemana se honraba con su vellorí de lo más fino. Tenía en su casa una ama que pasaba de los cuarenta, y una sobrina que no llegaba a los veinte, y un mozo de campo y plaza, que así ensillaba el rocín como tomaba la podadera. Frisaba la edad de nuestro hidalgo con los cincuenta años; era de complexión recia, seco de carnes, enjuto de rostro, gran madrugador y amigo de la caza. Quieren decir que tenía el sobrenombre de Quijada, o Quesada, que en esto hay alguna diferencia en los autores que deste caso escriben; aunque, por conjeturas verosímiles, se deja entender que se llamaba Quejana. Pero esto importa poco a nuestro cuento; basta que en la narración dél no se salga un punto de la verdad."]
        (for [word (.split quijote " " ) :when (re-matches #"\w+" word)]
          word)))

(defn tweet-channel [search]
  (let [out (chan)

        [ws-in ws-out] (ws-chan  "ws://localhost:9090")]
    (go-loop []
          (>! ws-out search)
          (.log js/console "Receiving")
          (let [timer (timeout 10000)
                words (to-chan
                       (alt! timer ([_] (.log js/console "plan b") (plan-b))
                             ws-in ([data] (.log js/console "ws-in")(tweet-seq data))))]
            (<! (blocking-pipe words out))
          (recur)))
    out))

