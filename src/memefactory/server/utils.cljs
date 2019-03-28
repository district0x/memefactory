(ns memefactory.server.utils
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.config :refer [config]]
   [district.server.web3 :as web3]
   [cljs.nodejs :as nodejs]
   [cljs.reader :refer [read-string]]
   [taoensso.timbre :as log]))

(defonce fs (nodejs/require "fs"))

(defn now-in-seconds []
  ;; if we are in dev we use blockchain timestamp so we can
  ;; increment it by hand, and also so we don't need block mining
  ;; in order to keep js time and blockchain time close
  (if (= :blockchain (:time-source @config))
    (->> (web3-eth/block-number @web3/web3) (web3-eth/get-block @web3/web3) :timestamp)
    (quot (.getTime (js/Date.)) 1000)))

(defn load-edn-file [file]
  (try
    (-> (.readFileSync fs file)
        .toString
        read-string)
    (catch js/Error e
      (log/warn (str "Couldn't load edn file " file) ::load-edn-file)
      nil)))

(defn save-to-edn-file [content file]
  (.writeFileSync fs file (pr-str content)))

(defn uninstall-filter [f]
  (web3-eth/stop-watching! f
                           (fn [err]
                             (let [id (-> f  .-filterId)]
                               (if err
                                 (log/error "Error uninstalling past event filter" {:error err :filter-id id})
                                 (log/info "Uninstalled past event filter" {:filter-id id}))))))
