(ns memefactory.server.twitter-bot
  (:require [bignumber.core :as bn]
            [camel-snake-kebab.core :as cs]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.core.async :as async]
            [cljs.nodejs :as nodejs]
            [district.format :as format]
            [district.server.config :as config]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [district.server.web3-events :as web3-events]
            [district.shared.async-helpers :refer [<? safe-go]]
            [goog.string :as gstring]
            [memefactory.server.conversion-rates :as conversion-rates]
            [memefactory.server.db :as db]
            [memefactory.server.ipfs :as ipfs]
            [memefactory.server.utils :as server-utils]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(def Twitter (nodejs/require "twitter"))
(def fs (nodejs/require "fs"))
(def tar-fs (nodejs/require "tar-fs"))
(def param-change-url "https://memefactory.io/param-change")

(defn upload-file-to-twitter [twitter-obj file-content]
  (let [out-ch (async/promise-chan)]
    (try
      (.post twitter-obj
             "media/upload"
             #js {:media file-content}
             (fn [error media _]
               (if error
                 (async/put! out-ch (js/Error. error))
                 (async/put! out-ch (.-media_id_string media)))))
      (catch js/Error e
        (async/put! out-ch e)))
    out-ch))

(defn tweet [twitter-obj
             {:keys [text media-id]}
             {:keys [just-log-tweet?]}]

  (if just-log-tweet?
    (log/info "Fakely twitting " {:text text :media-id media-id} ::tweet)

    ;; really create a tweet
    (do
      (log/info "Twitting " {:text text :media-id media-id} ::tweet)
      (.post twitter-obj
             "statuses/update"
             (cond-> {:status text}
               media-id (assoc :media_ids media-id)
               true     clj->js)
             (fn [error _ _]
               (if error
                 (log/error "Error sending tweet" {:error error} ::tweet)
                 (log/debug "Successfully twitted " {:text text :media-id media-id} ::tweet)))) )))

;; TODO: Figure out how to do this without going thru the filesystem
;; so it is less error prone
(defn first-tar-obj
  "Given a buffer with tar content, returns a promise that will yield
  another buffer with the first object inside the tar"
  [buffer write-directory]
  (let [out-ch (async/promise-chan)]
    (try
      (let [;; NOTE: Maybe instead of a random id we can create a folder with the same name as the ipfs hash
            ;; easier to debug
            tmp-name (str (random-uuid))
            ;; TODO : ensure slash
            tar-file (str (format/ensure-trailing-slash write-directory) tmp-name ".tar")
            img-tmp-dir (str (format/ensure-trailing-slash write-directory) tmp-name)
            extract-stream (.extract tar-fs img-tmp-dir)]
        (.writeFileSync fs tar-file buffer)
        (.pipe (.createReadStream fs tar-file) extract-stream)
        (.on extract-stream "finish"
             (fn []
               (async/put! out-ch (.readFileSync fs (str img-tmp-dir "/" (aget (.readdirSync fs img-tmp-dir) 0)))))))
      (catch :default e
        (async/put! out-ch e)))
    out-ch))

(defn ensure-media-uploaded [twitter-obj {:keys [image-hash registry-entry]} {:keys [just-log-tweet? write-directory]
                                                                              :or {write-directory "/tmp/memefactory"}}]
  (safe-go
   (if-let [media-id (db/get-meme-media-id registry-entry)]
     media-id
     (let [ipfs-hash (or image-hash
                         (:meme/image-hash (db/get-meme registry-entry)))
           _ (log/info "Uploading media " {:ipfs-hash ipfs-hash} ::ensure-media-uploaded)
           image-tar-file-content (<? (server-utils/get-ipfs-binary-file ipfs-hash))]
       (when-not just-log-tweet?
         (let [image-file-content (<? (first-tar-obj image-tar-file-content write-directory))
               media-id (<? (upload-file-to-twitter twitter-obj image-file-content))]
           media-id))))))

(defn tweet-meme-submitted [twitter-obj opts {:keys [:registry-entry :meta-hash]
                                              :as ev}]
  (safe-go
   (log/info "Twitter bot processing meme submitted event " ev ::tweet-meme-submitted)
   (let [meme-meta (<? (server-utils/get-ipfs-meta @ipfs/ipfs (web3-utils/to-ascii @web3 meta-hash)))
         {:keys [name image-hash]} meme-meta
         image-hash (server-utils/get-hash-from-ipfs-url image-hash)
         media-id (<? (ensure-media-uploaded twitter-obj {:image-hash image-hash} opts))
         meme-detail-url (str "https://memefactory.io/meme-detail/" registry-entry)
         text (rand-nth [(gstring/format "Introducing '%s', The latest submission to vie for a place in the DANK registry. %s" name meme-detail-url)
                         (gstring/format "The newest entry to the DANK registry, meet '%s'. Will it pass the bar? %s" name meme-detail-url)])]
     (log/info "Media uploaded, we got " {:media-id media-id} ::tweet-meme-submitted)
     (when media-id (db/save-meme-media-id! registry-entry (str media-id)))
     (tweet twitter-obj
            {:text text
             :media-id media-id}
            opts))))

(defn tweet-meme-challenged [twitter-obj opts {:keys [:registry-entry] :as ev}]
  (safe-go
   (log/info "Twitter bot processing meme challenged event " ev ::tweet-meme-challenged)
   (let [meme-detail-url (str "https://memefactory.io/meme-detail/" registry-entry)
         title (:meme/title (db/get-meme registry-entry))
         text (rand-nth [(gstring/format "A challenger appears... '%s' has had it's place in the DANK registry contested. DANK or STANK? %s" title meme-detail-url)
                         (gstring/format "%s has been challenged. DANK or STANK? Vote today %s" title meme-detail-url)])
         media-id (<? (ensure-media-uploaded twitter-obj {:registry-entry registry-entry} opts))]
     (tweet twitter-obj
            {:text text
             :media-id media-id}
            opts))))

;; We need to watch out here tweet it just once, even if more cards of the same meme were offered at once.
(def memes-offered-already-tweeted (atom #{}))

(defn tweet-meme-offered [twitter-obj opts {:keys [:token-id :block-number] :as ev}]
  (safe-go
   (log/info "Twitter bot processing meme offered event " ev ::tweet-meme-offered)
   (let [{:keys [:reg-entry/address :meme/title]} (db/get-meme-by-token-id token-id)
         meme-and-block [address block-number]]
     (when-not (contains? @memes-offered-already-tweeted meme-and-block)
       (let [meme-detail-url (str "https://memefactory.io/meme-detail/" address)
             text (rand-nth [(gstring/format "The exalted '%s' has been offered for sale. Get em while they last! %s" title meme-detail-url)
                             (gstring/format "Fresh off the factory lines, '%s' is up the latest sell offering on Meme Factory. Grab yours today! %s" title meme-detail-url)])
             media-id (<? (ensure-media-uploaded twitter-obj {:registry-entry address} opts))]
         (tweet twitter-obj
                {:text text
                 :media-id media-id}
                opts)
         (swap! memes-offered-already-tweeted conj meme-and-block))))))

(defn tweet-meme-auction-bought [twitter-obj opts {:keys [:meme-auction :price] :as ev}]
  (safe-go
   (log/info "Twitter bot processing auction bought event " ev ::tweet-meme-auction-bought)
   (let [{:keys [:reg-entry/address :meme/title]} (-> meme-auction
                                                      db/get-meme-by-auction-address)
         meme-detail-url (str "https://memefactory.io/meme-detail/" address)
         price-matic (bn/number (web3-utils/from-wei @web3 price :ether))
         formatted-price-matic (format/format-token  price-matic
                                                 {:max-fraction-digits 3
                                                  :min-fraction-digits 2
                                                  :token "MATIC"})
         price-dolar (* (conversion-rates/get-cached-rate-sync :MATIC :USD) price-matic)
         formatted-price-dolar (format/format-currency price-dolar {:currency "USD"})
         text (rand-nth [(gstring/format "'%s' was just purchased for [%s] [%s]. Find, create, and sell more rare collectibles only on Meme Factory! %s"
                                         title
                                         formatted-price-matic
                                         formatted-price-dolar
                                         meme-detail-url)
                         (gstring/format "SOLD! '%s' auctions for a profit of [%s] [%s]. Buy and sell your own provably rare memes on Meme Factory! %s"
                                         title
                                         formatted-price-matic
                                         formatted-price-dolar
                                         meme-detail-url)])
         media-id (<? (ensure-media-uploaded twitter-obj {:registry-entry address} opts))]
     (tweet twitter-obj
            {:text text
             :media-id media-id}
            opts))))


(defn get-param-name[db key]
  (let [db-param (smart-contracts/contract-address :param-change-registry-db)
        db-meme (smart-contracts/contract-address :meme-registry-db)
        param-type (cond
                     (= db-param db) "Parameter"
                     (= db-meme db) "Meme"
                     :else "")]
    (str param-type " " (gstring/replaceAll (cs/->Camel_Snake_Case key) "_" " "))))


(defn param-change-submitted [twitter-obj opts {:keys [:meta-hash :db :key] :as ev}]
  (safe-go
    (log/info "Twitter bot processing param submitted event " ev ::param-change-submitted)
    (let [param-name (get-param-name db key)
          ;{:keys [reason]} (<? (server-utils/get-ipfs-meta @ipfs/ipfs (web3-utils/to-ascii @web3 meta-hash)))
          text (rand-nth [(gstring/format "A change has been proposed to '%s'. Will it be accepted? %s" param-name param-change-url)
                          (gstring/format "There is a proposal to change '%s'. Would you challenge it? %s" param-name param-change-url)])
           ]
      (tweet twitter-obj
             {:text text}
             opts))))

(defn param-change-challenged [twitter-obj opts {:keys [:registry-entry] :as ev}]
  (safe-go
    (log/info "Twitter bot processing param challenged event " ev ::param-change-challenged)
    (let [{:keys [:param-change/db :param-change/key]} (db/get-param-change registry-entry)
          param-name (get-param-name db key)
          text (rand-nth [(gstring/format "A challenger appears... Change to '%s' has had it's place in the DANK registry contested. DANK or STANK? %s" param-name param-change-url)
                          (gstring/format "Change proposal '%s' has been challenged. DANK or STANK? Vote today %s" param-name param-change-url)])]
    (tweet twitter-obj
           {:text text}
           opts))))


(defn- dispatcher [twitter-obj opts callback]
  (fn [_ {:keys [:latest-event? :args :block-number]}]
    (when latest-event?
      (callback twitter-obj opts (assoc args :block-number block-number)))))

(defn start [{:keys [consumer-key consumer-secret access-token-key access-token-secret] :as opts}]
  (let [twitter-obj (Twitter. #js {:consumer_key consumer-key
                                   :consumer_secret consumer-secret
                                   :access_token_key access-token-key
                                   :access_token_secret access-token-secret})
        callback-ids
        [(web3-events/register-callback! :meme-registry/meme-constructed-event (dispatcher twitter-obj opts tweet-meme-submitted))
         (web3-events/register-callback! :meme-registry/challenge-created-event (dispatcher twitter-obj opts tweet-meme-challenged))
         (web3-events/register-callback! :meme-auction-factory/meme-auction-started-event (dispatcher twitter-obj opts tweet-meme-offered))
         (web3-events/register-callback! :meme-auction-factory/meme-auction-buy-event (dispatcher twitter-obj opts tweet-meme-auction-bought))
         (web3-events/register-callback! :param-change-registry/param-change-constructed-event (dispatcher twitter-obj opts param-change-submitted))
         (web3-events/register-callback! :param-change-registry/challenge-created-event (dispatcher twitter-obj opts param-change-challenged))]]
    {:callback-ids callback-ids
     :twitter-obj twitter-obj}))

(defn stop [twitter-bot]
  (web3-events/unregister-callbacks! (:callback-ids @twitter-bot)))

(defstate twitter-bot
  :start (start (merge (:twitter-bot @config/config)
                       (:twitter-bot (mount/args))))
  :stop (stop twitter-bot))
