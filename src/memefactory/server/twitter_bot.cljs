(ns memefactory.server.twitter-bot
  (:require
   [cljs.nodejs :as nodejs]
   [cljs-time.coerce :as time-coerce]
   [cljs-web3.core :as web3]
   [cljs-time.core :as t]
   [district.encryption :as encryption]
   [district.format :as format]
   [district.sendgrid :refer [send-email]]
   [district.server.config :as config]
   [district.server.config :refer [config]]
   [district.server.logging]
   [district.server.web3-events :refer [register-callback! unregister-callbacks!]]
   [district.shared.error-handling :refer [try-catch try-catch-throw]]
   [district.time :as time]
   [goog.format.EmailAddress :as email-address]
   [memefactory.server.contract.district0x-emails :as district0x-emails]
   [memefactory.server.db :as db]
   [memefactory.server.emailer.templates :as templates]
   [district.shared.async-helpers :refer [promise->]]
   [memefactory.server.utils :as server-utils]
   [memefactory.server.ipfs :as ipfs]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   [goog.string :as gstring]
   [print.foo :refer [look] :include-macros true]
   [bignumber.core :as bn]
   [memefactory.server.conversion-rates :as conv-rates]))

(def Twitter (nodejs/require "twitter"))
(def fs (nodejs/require "fs"))
(def tar-fs (nodejs/require "tar-fs"))

(defn upload-file-to-twitter [twitter-obj file-content]
  (js/Promise. (fn [resolve reject]
                 (try
                   (.post twitter-obj
                          "media/upload"
                          #js {:media file-content}
                          (fn [error media response]
                            (if error
                              (reject error)
                              (resolve (.-media_id_string media)))))
                   (catch :default e
                     (reject e))))))

(defn tweet [twitter-obj {:keys [text media-id] :as tweet} {:keys [just-log-tweet?]}]

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
             (fn [error tw resp]
               (if error
                 (log/error "Error sending tweet" {:error error} ::tweet)
                 (log/debug "Successfully twitted " {:text text :media-id media-id} ::tweet)))) )))

;; TODO: Figure out how to do this without going thru the filesystem
;; so it is less error prone
(defn first-tar-obj
  "Given a buffer with tar content, returns a promise that will yield
  another buffer with the first object inside the tar"
  [buffer]

  (js/Promise.
   (fn [resolve reject]

     (try
       (let [tmp-dir "/tmp/memefactory"
             ;; NOTE: Maybe instead of a random id we can create a folder with the same name as the ipfs hash
             ;; easier to debug
             tmp-name (str (random-uuid))
             tar-file (str tmp-dir "/" tmp-name ".tar")
             img-tmp-dir (str tmp-dir "/" tmp-name)
             extract-stream (.extract tar-fs img-tmp-dir)]
         (.writeFileSync fs tar-file buffer)
         (.pipe (.createReadStream fs tar-file) extract-stream)
         (.on extract-stream "finish"
              (fn []
                (resolve (.readFileSync fs (str img-tmp-dir "/" (aget (.readdirSync fs img-tmp-dir) 0)))))))
       (catch :default e
         (reject e))))))

(defn ensure-media-uploaded [twitter-obj {:keys [image-hash registry-entry]} {:keys [just-log-tweet?]}]
  (js/Promise.
   (fn [resolve reject]
     (if-let [media-id (db/get-meme-media-id registry-entry)]
       (resolve media-id)
       (let [ipfs-hash (-> (or image-hash
                               (:meme/image-hash (db/get-meme registry-entry)))
                           (clojure.string/split #"/")
                           butlast
                           (->> (clojure.string/join "/")))]
         (log/info "Uploading media " {:ipfs-hash ipfs-hash} ::ensure-media-uploaded)
         (promise-> (server-utils/get-ipfs-binary-file ipfs-hash)
                    (fn [image-tar-file-content]
                      (when-not just-log-tweet?
                        (promise-> (first-tar-obj image-tar-file-content)
                                   (fn [image-file-content]
                                     (promise-> (upload-file-to-twitter twitter-obj image-file-content)
                                                (fn [media-id] (resolve media-id)))))))))))))

(defn tweet-meme-submitted [twitter-obj opts {:keys [:registry-entry :timestamp :creator :meta-hash
                                                                :total-supply :version :deposit :challenge-period-end]
                                              :as ev}]
  (log/debug "Twitter bot processing meme submitted event " ev ::tweet-meme-submitted)
  (promise-> (server-utils/get-ipfs-meta @ipfs/ipfs (web3/to-ascii meta-hash))
             (fn [{:keys [title image-hash] :as meme-meta}]
               (promise-> (ensure-media-uploaded twitter-obj {:image-hash image-hash} opts)
                          (fn [media-id]
                            (let [meme-detail-url (str "https://memefactory.io/meme-detail/" registry-entry)
                                  text (rand-nth [(gstring/format "Introducing '%s', The latest submission to vie for a place in the DANK registry. %s" title meme-detail-url)
                                                  (gstring/format "The newest entry to the DANK registry, meet '%s'. Will it pass the bar? %s" title meme-detail-url)])]
                              (log/info "Media uploladed, we got " {:media-id media-id} ::tweet-meme-submitted)
                              (when media-id (db/save-meme-media-id! registry-entry (str media-id)))
                              (tweet twitter-obj
                                     {:text text
                                      :media-id media-id}
                                     opts)))))))

(defn tweet-meme-challenged [twitter-obj opts {:keys [:registry-entry :challenger :commit-period-end
                                                      :reveal-period-end :reward-pool :metahash :timestamp :version] :as ev}]
  (log/debug "Twitter bot processing meme challenged event " ev ::tweet-meme-challenged)
  (let [meme-detail-url (str "https://memefactory.io/meme-detail/" registry-entry)
        title (:meme/title (db/get-meme registry-entry))
        text (rand-nth [(gstring/format "A challenger appears... '%s' has had it's place in the DANK registry contested. DANK or STANK? %s" title meme-detail-url)
                        (gstring/format "%s has been challenged. DANK or STANK? Vote today %s" title meme-detail-url)])]
    (promise-> (ensure-media-uploaded twitter-obj {:registry-entry registry-entry} opts)
               (fn [media-id]
                 (tweet twitter-obj
                        {:text text
                         :media-id media-id}
                        opts)))))

;; We need to watch out here tweet it just once, even if more cards of the same meme were offered at once.
(def memes-offered-already-tweeted (atom #{}))
(defn tweet-meme-offered [twitter-obj opts {:keys [:meme-auction :timestamp :meme-auction :token-id :seller :start-price :end-price
                                                              :duration :description :started-on :block-number] :as ev}]
  (log/debug "Twitter bot processing meme offered event " ev ::tweet-meme-offered)
  (let [{:keys [:reg-entry/address :meme/title]} (db/get-meme-by-token-id (bn/number token-id))
        meme-and-block [address block-number]]
    (when-not (contains? @memes-offered-already-tweeted meme-and-block)
      (let [meme-detail-url (str "https://memefactory.io/meme-detail/" address)
            text (rand-nth [(gstring/format "The exalted '%s' has been offered for sale. Get em while they last! %s" title meme-detail-url)
                            (gstring/format "Fresh off the factory lines, '%s' is up the latest sell offering on Meme Factory. Grab yours today! %s" title meme-detail-url)])]
        (promise-> (ensure-media-uploaded twitter-obj {:registry-entry address} opts)
                   (fn [media-id]
                     (tweet twitter-obj
                            {:text text
                             :media-id media-id}
                            opts)))
        (swap! memes-offered-already-tweeted conj meme-and-block)))))

(defn tweet-meme-auction-bought [twitter-obj opts {:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds] :as ev}]
  (log/debug "Twitter bot processing auction bought event " ev ::tweet-meme-auction-bought)
  (let [{:keys [:reg-entry/address :meme/title]} (-> meme-auction
                                                     db/get-meme-by-auction-address)
        meme-detail-url (str "https://memefactory.io/meme-detail/" address)
        price-eth (bn/number (web3/from-wei price :ether))
        formatted-price-eth (format/format-eth  price-eth
                                                {:max-fraction-digits 3
                                                 :min-fraction-digits 2})
        price-dolar (* (conv-rates/get-cached-rate-sync :ETH :USD) price-eth)
        formatted-price-dolar (format/format-currency price-dolar {:currency "USD"})
        text (rand-nth [(gstring/format "'%s' was just purchased for [%s] [%s]. Find, create, and sell more rare collectibles only on Meme Factory! %s"
                                        title
                                        formatted-price-eth
                                        formatted-price-dolar
                                        meme-detail-url)
                        (gstring/format "SOLD! '%s' auctions for a profit of [%s] [%s]. Buy and sell your own provably rare memes on Meme Factory! %s"
                                        title
                                        formatted-price-eth
                                        formatted-price-dolar
                                        meme-detail-url)])]
    (promise-> (ensure-media-uploaded twitter-obj {:registry-entry address} opts)
               (fn [media-id]
                 (tweet twitter-obj
                        {:text text
                         :media-id media-id}
                        opts)))))

(defn- dispatcher [twitter-obj opts callback]
  (fn [_ {:keys [:latest-event? :args :block-number] :as ev}]
    (when latest-event?
      (callback twitter-obj opts (assoc args :block-number block-number)))))


(defn start [{:keys [consumer-key consumer-secret access-token-key access-token-secret] :as opts}]
  (let [twitter-obj (Twitter. #js {:consumer_key consumer-key
                                   :consumer_secret consumer-secret
                                   :access_token_key access-token-key
                                   :access_token_secret access-token-secret})
        callback-ids
        [(register-callback! :meme-registry/meme-constructed-event (dispatcher twitter-obj opts tweet-meme-submitted))
         (register-callback! :meme-registry/challenge-created-event (dispatcher twitter-obj opts tweet-meme-challenged))
         (register-callback! :meme-auction-factory/meme-auction-started-event (dispatcher twitter-obj opts tweet-meme-offered))
         (register-callback! :meme-auction-factory/meme-auction-buy-event (dispatcher twitter-obj opts tweet-meme-auction-bought))]]
    {:callback-ids callback-ids
     :twitter-obj twitter-obj}))


(defn stop [twitter-bot]
  (unregister-callbacks! (:callback-ids @twitter-bot)))


(defstate twitter-bot
  :start (start (merge (:twitter-bot @config)
                       (:twitter-bot (mount/args))))
  :stop (stop twitter-bot))
