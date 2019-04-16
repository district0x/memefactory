(ns memefactory.server.pinner
  (:require
    [cljs-ipfs-api.core :as ipfs-api]
    [cljs-ipfs-api.files :as ipfs-files]
    [cljs-ipfs-api.pin :as pin]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs.nodejs :as nodejs]
    [clojure.pprint :refer [print-table]]
    [district.server.config :refer [config]]
    [district.server.logging]
    [district.server.smart-contracts :refer [replay-past-events]]
    [district.server.web3 :refer [web3]]
    [goog.date.Date]
    [memefactory.server.contract.registry :as registry]
    [memefactory.server.ipfs :as ipfs]
    [memefactory.server.macros :refer [promise->]]
    [memefactory.server.utils :as server-utils]
    [memefactory.shared.smart-contracts]
    [mount.core :as mount :refer [defstate]]
    [print.foo :refer [look] :include-macros true]
    [taoensso.timbre :as log]))


(declare pinner)

(defn on-jsload []
  (mount/stop pinner)
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :web3 {:url "http://localhost:8549"}
                            :ipfs {:host "http://127.0.0.1:5001"
                                   :endpoint "/api/v0"
                                   :gateway "http://127.0.0.1:8080/ipfs"}
                            :smart-contracts {:contracts-var #'memefactory.shared.smart-contracts/smart-contracts}}}})
    (mount/start)
    (#(log/warn "Started" {:components %
                           :config @config}))))


(defn- last-block-number []
  (web3-eth/block-number @web3))


(defn meme-constructed-event [err {:keys [:args]}]
  (let [{:keys [:meta-hash]} args
        meta-hash (web3/to-ascii meta-hash)]

    (cond
      err
      (log/error (str "Error in meme constructed event " err) ::event-error)

      (not (ipfs/ipfs-hash? meta-hash))
      (log/error (str "Meme meta hash is not valid IPFS hash " meta-hash) ::invalid-meta-hash)

      :else
      (promise->
        (server-utils/get-ipfs-meta @ipfs/ipfs meta-hash)
        (fn [meme-meta]
          (let [{:keys [:image-hash]} meme-meta]

            (if-not (ipfs/ipfs-hash? image-hash)
              (log/error (str "Meme image hash is not valid IPFS hash " image-hash) ::invalid-image-hash)

              (do
                (pin/add meta-hash
                         (fn [err]
                           (if err
                             (log/error (str "Pinning meme meta hash failed " meta-hash) ::pin-meta-hash)
                             (log/info (str "Pinned meme meta hash " meta-hash) ::pin-meta-hash))))

                (pin/add image-hash
                         (fn [err]
                           (if err
                             (log/error (str "Pinning meme image hash failed " image-hash " " err) ::pin-image-hash)
                             (log/info (str "Pinned meme image hash " image-hash) ::pin-image-hash))))))))))))



(defn challenge-created-event [err {:keys [:args]}]
  (let [{:keys [:metahash]} args
        meta-hash (web3/to-ascii metahash)]

    (cond
      err
      (log/error (str "Error in challenge created event " err) ::event-error)

      (not (ipfs/ipfs-hash? meta-hash))
      (log/error (str "Challenge meta hash is not IPFS valid hash " meta-hash) ::invalid-meta-hash)

      :else
      (pin/add meta-hash
               (fn [err]
                 (if err
                   (log/error (str "Pinning challenge meta hash failed " meta-hash " " err) ::pin-meta-hash)
                   (log/info (str "Pinned challenge meta hash " meta-hash) ::pin-meta-hash)))))))


(defn start [opts]
  (when-not (web3/connected? @web3)
    (throw (js/Error. "Can't connect to Ethereum node")))

  [(registry/meme-constructed-event [:meme-registry :meme-registry-fwd] "latest" meme-constructed-event)
   (-> (registry/meme-constructed-event [:meme-registry :meme-registry-fwd] {:from-block 0 :to-block "latest"} identity)
     (replay-past-events meme-constructed-event))

   (registry/challenge-created-event [:meme-registry :meme-registry-fwd] "latest" challenge-created-event)
   (-> (registry/challenge-created-event [:meme-registry :meme-registry-fwd] {:from-block 0 :to-block "latest"} identity)
     (replay-past-events challenge-created-event))])


(defn stop []
  (doseq [filter (remove nil? @pinner)]
    (web3-eth/stop-watching! filter (fn []))))


(defstate pinner
  :start (start (merge (:pinner @config)
                       (:pinner (mount/args))))
  :stop (stop))


(defn -main [& _]
  (on-jsload))


(set! *main-cli-fn* -main)
