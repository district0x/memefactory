(ns memefactory.server.conversion-rates
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [cljs.nodejs :as nodejs]
            [goog.string :as gstring]
            [district.server.config :refer [config]]))

(def Https (nodejs/require "https"))

(declare start)
(declare stop)

(defstate conversion-rates
  :start (start (merge (:conversion-rates @config)
                       (:conversion-rates (mount/args))))
  :stop (stop conversion-rates))


(defn get-rate [currency1 currency2]
  (js/Promise.
   (fn [resolve reject]
     (let [data (atom "")]
       (.get Https (gstring/format "https://min-api.cryptocompare.com/data/pricemulti?fsyms=%s&tsyms=%s"
                                   (name currency1)
                                   (name currency2))
             (fn [resp]
               (.on resp "data" (fn [c] (swap! data str c)))
               (.on resp "end"
                    (fn []
                      (try
                        (let [rates (-> (js->clj (js/JSON.parse @data))
                                       vals
                                       first
                                       vals
                                       first)]

                         (resolve rates))
                        (catch js/Error e
                                (reject e)))))))))))

(defn get-cached-rate-sync
  "Currencies should be a keyword like :ETH, :USD, :EUR, etc"
  [currency1 currency2]
  (get @(:rates @conversion-rates) [currency1 currency2]))

(defn start [{:keys [update-interval]}]
  (let [rates (atom {})
        interval-ptr (js/setInterval (fn []
                                       (-> (get-rate :ETH :USD)
                                           (.then (fn [rate]
                                                    (swap! rates assoc [:ETH :USD] rate)))
                                           (.catch (fn [e] (log/error e)))))
                                     (or update-interval
                                         ;; 10 min default
                                         (* 10 60 1000)))]

    {:interval-ptr interval-ptr
     :rates rates}))


(defn stop [conversion-rates]
  (js/clearInterval (:interval-ptr @conversion-rates))
  )
