(ns memefactory.server.pinner
  (:require
    [cljs-ipfs-api.pin :as pin]
    [cljs-web3.core :as web3]
    [district.server.config :refer [config]]
    [district.server.logging]
    [district.server.web3 :refer [web3]]
    [district.server.web3-events :refer [register-callback! unregister-callbacks!]]
    [goog.date.Date]
    [memefactory.server.ipfs :as ipfs]
    [district.shared.async-helpers :refer [promise->]]
    [memefactory.server.utils :as server-utils]
    [mount.core :as mount :refer [defstate]]
    [print.foo :refer [look] :include-macros true]
    [taoensso.timbre :as log]))

(defn- dispatcher [callback]
  (fn [err event]
    (js/Promise.
     (fn [resolve reject]
       (callback err event)
       (resolve ::dispatched)))))

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
                             (log/info (str "Pinned meme image hash " image-hash) ::pin-image-hash))))))))

        ))))


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
  (when-not (:disabled? opts)
    (register-callback! :meme-registry/meme-constructed-event (dispatcher meme-constructed-event) ::meme-constructed-event)
    (register-callback! :meme-registry/challenge-created-event (dispatcher challenge-created-event) ::challenge-created-event))
  opts)


(defn stop [pinner]
  (when-not (:disabled? @pinner)
    (unregister-callbacks! [::meme-constructed-event ::challenge-created-event])))


(defstate pinner
  :start (start (merge (:pinner @config)
                       (:pinner (mount/args))))
  :stop (stop pinner))
