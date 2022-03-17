(ns memefactory.server.utils
  (:require [cljs-ipfs-api.files :as ipfs-files]
            [cljs-web3-next.eth :as web3-eth]
            [cljs.nodejs :as nodejs]
            [cljs.reader :refer [read-string]]
            [clojure.string :as string]
            [district.server.config :refer [config]]
            [district.server.web3 :refer [web3]]
            [taoensso.timbre :as log]
            [district.shared.async-helpers :refer [promise->]]))

(defonce fs (nodejs/require "fs"))

(defn now []
  (.getTime (js/Date.)))

(defn now-in-seconds []
  ;; if we are in dev we use blockchain timestamp so we can
  ;; increment it by hand, and also so we don't need block mining
  ;; in order to keep js time and blockchain time close
  ;; returns a Promise
  (if (= :blockchain (:time-source @config))
    (promise-> (web3-eth/get-block-number @web3)
               #(web3-eth/get-block @web3 % false)
               #(-> % (js->clj :keywordize-keys true) :timestamp))
    (js/Promise.resolve (quot (now) 1000))))

(defn load-edn-file [file & read-opts]
  (try
    (->> (.readFileSync fs file)
         .toString
         (read-string read-opts))
    (catch :default e
      (log/warn (str "Couldn't load edn file " file) {:error e} ::load-edn-file))))

(defn save-to-edn-file [content file]
  (.writeFileSync fs file (pr-str content)))

(defn append-line-to-file [file-path line]
  (.appendFileSync fs
                   file-path
                   (str line "\n")))

(defn delete-file [file-path]
  (try
    (.unlinkSync fs file-path)
    (catch :default e
      (log/error (str "Couldn't delete file " file-path ) {:error e}))))

(defn- parse-meta [{:keys [:content :on-success :on-error]}]
  (try
    (-> (re-find #".+?(\{.+\})" content)
        second
        js/JSON.parse
        (js->clj :keywordize-keys true)
        on-success)
    (catch :default e
      (on-error e))))

(defn get-ipfs-meta [conn meta-hash]
  (js/Promise.
   (fn [resolve reject]
     (log/info (str "Downloading: " "/ipfs/" meta-hash) ::get-ipfs-meta)
     (ipfs-files/fget (str "/ipfs/" meta-hash)
                      {:req-opts {:compress false}}
                      (fn [err content]
                        (cond
                          err
                          (let [err-txt "Error when retrieving metadata from ipfs"]
                            (log/error err-txt (merge {:meta-hash meta-hash
                                                       :connection conn
                                                       :error err})
                                       ::get-ipfs-meta)
                            (reject (str err-txt " : " err)))

                          (empty? content)
                          (let [err-txt "Empty ipfs content"]
                            (log/error err-txt {:meta-hash meta-hash
                                                :connection conn} ::get-ipfs-meta)
                            (reject err-txt))

                          :else (parse-meta {:content content
                                             :on-success resolve
                                             :on-error reject})))))))

(defn get-ipfs-binary-file [file-hash]
  (js/Promise.
   (fn [resolve reject]
     (log/info (str "Downloading: " "/ipfs/" file-hash) ::get-ipfs-file)
     (ipfs-files/fget (str "/ipfs/" file-hash)
                      {:req-opts {:compress false
                                  :archive :false}
                       :binary? true}
                      (fn [err content]

                        (cond
                          err
                          (let [err-txt "Error when retrieving file from ipfs"]
                            (log/error err-txt (merge {:file-hash file-hash
                                                       :error err})
                                       ::get-ipfs-file)
                            (reject (str err-txt " : " err)))

                          :else (resolve content)))))))


(defn get-hash-from-ipfs-url [ipfs-url]
  (if (string/starts-with? ipfs-url "ipfs://") (subs ipfs-url 7) ipfs-url))
