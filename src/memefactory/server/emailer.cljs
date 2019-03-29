(ns memefactory.server.emailer
  (:require
   [cljs-time.core :as t]
   [cljs-time.coerce :as time-coerce]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.core :as web3]
   [district.encryption :as encryption]
   [district.format :as format]
   [district.sendgrid :refer [send-email]]
   [district.server.config :as config]
   [district.shared.error-handling :refer [try-catch]]
   [district.time :as time]
   [goog.format.EmailAddress :as email-address]
   [memefactory.server.contract.district0x-emails :as district0x-emails]
   [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
   [memefactory.server.contract.registry :as registry]
   [memefactory.server.db :as db]
   [memefactory.server.emailer.templates :as templates]
   [memefactory.server.macros :refer [promise->]]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log :refer [spy]]))

(declare start)
(declare stop)

(defstate emailer
  :start (start (merge (:emailer @config/config)
                       (:emailer (mount/args))))
  :stop (stop emailer))

(defn validate-email [base64-encrypted-email]
  (when-not (empty? base64-encrypted-email)
    (let [email (encryption/decode-decrypt (:private-key @emailer) base64-encrypted-email)]
      (when (email-address/isValidAddress email)
        email))))

(defn send-challenge-created-email [{:keys [:registry-entry :challenger :commit-period-end
                                            :reveal-period-end :reward-pool :metahash :timestamp :version] :as ev}]
  (try-catch
   (let [{:keys [:reg-entry/creator :meme/title :meme/image-hash] :as meme} (db/get-meme registry-entry)
         {:keys [:from :template-id :api-key :print-mode?]} @emailer
         root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
         ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))
         [unit value] (time/time-remaining-biggest-unit (t/now)
                                                        (-> commit-period-end time/epoch->long time-coerce/from-long))]
     (promise-> (district0x-emails/get-email {:district0x-emails/address creator})
                #(validate-email %)
                (fn [to] (if to
                           (do
                             (log/info "Sending meme challenged email" ev ::send-challenge-created-email)
                             (send-email {:from from
                                          :to to
                                          :subject (str title " was challenged!")
                                          :content (templates/challenge-created-email-body {:meme/title title
                                                                                            :meme-url (str root-url "meme-detail/" registry-entry)
                                                                                            :time-remaining (format/format-time-units {unit value})
                                                                                            })
                                          :substitutions {:header (str title " was challenged")
                                                          :button-title "Vote Now"
                                                          :button-href (str root-url "dankregistry/vote")
                                                          :meme-image-url (str ipfs-gateway-url image-hash)}
                                          :on-success #(log/info "Success sending meme challenged email" {:to to
                                                                                                          :registry-entry registry-entry
                                                                                                          :meme-title title}
                                                                 ::send-challenge-created-email)
                                          :on-error #(log/error "Error when sending meme challenged email" {:error %
                                                                                                            :event ev
                                                                                                            :meme meme
                                                                                                            :to to}
                                                                ::send-challenge-created-email)
                                          :template-id template-id
                                          :api-key api-key
                                          :print-mode? print-mode?}))
                           (log/warn "No email found for challenged meme creator" {:event ev :meme meme} ::send-challenge-created-email)))))))

(defn send-auction-bought-email [{:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds] :as ev}]
  (try-catch
   (let [{:keys [:meme-auction/seller :meme-auction/address] :as meme-auction} (db/get-meme-auction meme-auction)
         {:keys [:meme/title :meme/image-hash] :as meme} (db/get-meme-by-auction-address address)
         {:keys [:from :template-id :api-key :print-mode?]} @emailer
         root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
         ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))]
     (promise-> (district0x-emails/get-email {:district0x-emails/address seller})
                #(validate-email %)
                (fn [to] (if to
                           (send-email {:from from
                                        :to to
                                        :subject (str title " was bought!")
                                        :content (templates/meme-auction-bought-email-body {:meme/title title
                                                                                            :meme-url (str root-url "meme-detail/" (:reg-entry/address meme))})
                                        :substitutions {:header (str title " was bought!")
                                                        :button-title "My Memefolio"
                                                        :button-href (str root-url "memefolio/?tab=sold")
                                                        :meme-image-url (str ipfs-gateway-url image-hash)}
                                        :on-success #(log/info "Success sending offering bought email" {:to to
                                                                                                       :meme-auction meme-auction
                                                                                                       :meme-title title}
                                                               ::send-auction-bought-email)
                                        :on-error #(log/error "Error when sending offering-bought email" {:error %
                                                                                                         :event ev
                                                                                                         :meme-auction meme-auction
                                                                                                         :to to}
                                                              ::send-auction-bought-email)
                                        :template-id template-id
                                        :api-key api-key
                                        :print-mode? print-mode?})
                           (log/warn "No email found for meme seller" {:event ev :meme-auction meme-auction} ::send-auction-bought-email)))))))

(defn send-vote-reward-claimed-email [{:keys [:registry-entry :timestamp :version :voter :amount] :as ev}]
  (try-catch
   (let [{:keys [:meme/title :meme/image-hash] :as meme} (db/get-meme registry-entry)
         {:keys [:vote/option]} (db/get-vote {:reg-entry/address registry-entry :vote/voter voter} [:vote/option])
         {:keys [:from :template-id :api-key :print-mode?]} @emailer
         root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
         ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))]
     (promise-> (district0x-emails/get-email {:district0x-emails/address voter})
                #(validate-email %)
                (fn [to]
                  (if to
                    (do
                      (log/info "Sending vote reward received email" ev ::send-vote-reward-claimed-email)
                      (send-email {:from from
                                   :to to
                                   :subject "You received a vote reward"
                                   :content (templates/vote-reward-claimed-email-body {:meme/title title
                                                                                       :vote/option (case option
                                                                                                      1 "DANK"
                                                                                                      2 "STANK")
                                                                                       :amount (-> amount (web3/from-wei :ether) (format/format-token {:token "DANK"}))
                                                                                       :meme-url (str root-url "meme-detail/" registry-entry)})
                                   :substitutions {:header "Vote Reward"
                                                   :button-title "My Memefolio"
                                                   :button-href (str root-url "memefolio/?tab=curated")
                                                   :meme-image-url (str ipfs-gateway-url image-hash)}
                                   :on-success #(log/info "Success sending email" {:to to
                                                                                   :registry-entry registry-entry
                                                                                   :meme-title title}
                                                          ::send-vote-reward-claimed-email)
                                   :on-error #(log/error "Error when sending email" {:error %
                                                                                     :event ev
                                                                                     :meme meme
                                                                                     :to to}
                                                         ::send-vote-reward-claimed-email)
                                   :template-id template-id
                                   :api-key api-key
                                   :print-mode? print-mode?}))
                    (log/warn "No email found for voter" {:event ev :meme meme} ::send-vote-reward-claimed-email)))))))

(defn send-challenge-reward-claimed-email [{:keys [:registry-entry :timestamp :version :challenger :amount] :as ev}]
  (try-catch
   (let [{:keys [:meme/title :meme/image-hash] :as meme} (db/get-meme registry-entry)
         {:keys [:from :template-id :api-key :print-mode?]} @emailer
         root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
         ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))]
     (promise-> (district0x-emails/get-email {:district0x-emails/address challenger})
                #(validate-email %)
                (fn [to] (if to
                           (do (log/info "Sending chalenge reward received email" ev ::send-challenge-reward-claimed-email)
                               (send-email {:from from
                                            :to to
                                            :subject "You received a challenge reward"
                                            :content (templates/challenge-reward-claimed-email-body {:amount (-> amount (web3/from-wei :ether) (format/format-token {:token "DANK"}))
                                                                                                     :meme/title title
                                                                                                     :meme-url (str root-url
                                                                                                                    "meme-detail/"
                                                                                                                    registry-entry)})
                                            :substitutions {:header "Challenge Reward"
                                                            :button-title "My Memefolio"
                                                            :button-href (str root-url "memefolio/?tab=curated")
                                                            :meme-image-url (str ipfs-gateway-url image-hash)}
                                            :on-success #(log/info "Success sending challenge reward claimed email" {:to to
                                                                                                                     :registry-entry registry-entry
                                                                                                                     :title title}
                                                                   ::send-challenge-reward-claimed-email)
                                            :on-error #(log/error "Error when sending challenge reward claimed email" {:error %
                                                                                                                       :event ev
                                                                                                                       :meme meme
                                                                                                                       :to to}
                                                                  ::send-challenge-reward-claimed-email)
                                            :template-id template-id
                                            :api-key api-key
                                            :print-mode? print-mode?}))
                           (log/warn "No email found for challenger" {:event ev :meme meme} ::send-challenge-reward-claimed-email)))))))

(defn event-callback [f]
  (fn [error res]
    (if error
      ((:on-event-error @@#'memefactory.server.emailer/emailer) error)
      (f (:args res)))))

(defn start [{:keys [:api-key :private-key :print-mode? :on-event-error] :as opts}]

  (when-not private-key
    (throw (js/Error. ":private-key is required to start emailer")))

  (when-not on-event-error
    (throw (js/Error. ":on-event-error is required to start emailer")))

  (merge opts
         {:listeners
          [(registry/challenge-created-event [:meme-registry :meme-registry-fwd] "latest" (event-callback send-challenge-created-email))
           (meme-auction-factory/meme-auction-buy-event "latest" (event-callback send-auction-bought-email))
           (registry/challenge-reward-claimed-event [:meme-registry :meme-registry-fwd] "latest" (event-callback send-challenge-reward-claimed-email))
           (registry/vote-reward-claimed-event [:meme-registry :meme-registry-fwd] "latest" (event-callback send-vote-reward-claimed-email))]}))

(defn stop [emailer]
  (doseq [listener (remove nil? (:listeners @emailer))]
    (web3-eth/stop-watching! listener (fn [err])))
  (log/info "stopping emailer" {:state @emailer}))
