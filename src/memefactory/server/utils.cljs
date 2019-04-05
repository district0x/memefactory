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

(defn load-edn-file [file & read-opts]
  (try
    (->> (.readFileSync fs file)
         .toString
         (read-string read-opts))
    (catch js/Error e
      (log/warn (str "Couldn't load edn file " file) ::load-edn-file)
      nil)))

(defn save-to-edn-file [content file]
  (.writeFileSync fs file (pr-str content)))

(defn append-line-to-file [file-path line]
  (.appendFileSync fs
                   file-path
                   (str line "\n")))

(defn delete-file [file-path]
  (try
    (.unlinkSync fs file-path)
    (catch js/Error e
      (log/debug (str "Couldn't delete file " file-path " because of " e)))))
