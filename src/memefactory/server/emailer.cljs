(ns memefactory.server.emailer
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.encryption :as encryption]
   [district.sendgrid :refer [send-email]]
   [district.server.config :refer [config]]
   [district.shared.error-handling :refer [try-catch]]
   [goog.format.EmailAddress :as email-address]
   [memefactory.server.contract.district0x-emails :as district0x-emails]
   [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
   [memefactory.server.contract.registry :as registry]
   [memefactory.server.db :as db]
   [memefactory.server.emailer.templates :as templates]
   [memefactory.server.macros :refer [promise->]]
   [mount.core :as mount :refer [defstate]]
   [print.foo :refer [look] :include-macros true]
   [taoensso.timbre :as log]
   ))

(declare start)
(declare stop)

(defstate emailer
  :start (start (merge (:emailer @config)
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
   (let [{:keys [:reg-entry/creator :meme/title] :as meme} (db/get-meme registry-entry)
         {:keys [:from :template-id :api-key :print-mode?]} @emailer]
     (promise-> (district0x-emails/get-email {:district0x-emails/address creator})
                #(validate-email %)
                (fn [to] (if to
                           (send-email {:from from
                                        :to to
                                        :subject (str "Your meme has been challenged ")
                                        :content (templates/challenge-created-email-body meme)
                                        :substitutions {:header (str title " meme challenged")}
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
                                        :print-mode? print-mode?})
                           (log/warn "No email found for challenged meme creator" {:event ev :meme meme} ::send-challenge-created-email)))))))

(defn send-auction-bought-email [{:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds] :as ev}]
  (try-catch
   (let [{:keys [:meme-auction/seller :meme-auction/address] :as meme-auction} (db/get-meme-auction meme-auction)
         {:keys [:meme/title] :as meme} (db/get-meme-by-auction-address address)
         {:keys [:from :template-id :api-key :print-mode?]} @emailer]
     (promise-> (district0x-emails/get-email {:district0x-emails/address seller})
                #(validate-email %)
                (fn [to] (if to
                           (send-email {:from from
                                        :to to
                                        :subject (str "One of your auctions has been bought")
                                        :content (templates/meme-auction-bought-email-body meme-auction)
                                        :substitutions {:header (str "Your auction for " title  " has been bought")}
                                        :on-success #(log/info "Success sending auction bought email" {:to to
                                                                                                       :meme-auction meme-auction
                                                                                                       :meme-title title}
                                                               ::send-auction-bought-email)
                                        :on-error #(log/error "Error when sending auction-bought email" {:error %
                                                                                                         :event ev
                                                                                                         :meme-auction meme-auction
                                                                                                         :to to}
                                                              ::send-auction-bought-email)
                                        :template-id template-id
                                        :api-key api-key
                                        :print-mode? print-mode?})
                           (log/warn "No email found for meme auction seller" {:event ev :meme-auction meme-auction} ::send-auction-bought-email)))))))

(defn send-vote-reward-claimed-email [{:keys [:registry-entry :timestamp :version :voter :amount] :as ev}]
  (try-catch
   (let [{:keys [:meme/title] :as meme} (db/get-meme registry-entry)
         {:keys [:from :template-id :api-key :print-mode?]} @emailer]
     (promise-> (district0x-emails/get-email {:district0x-emails/address voter})
                #(validate-email %)
                (fn [to]
                  (if to
                    (send-email {:from from
                                 :to to
                                 :subject "You received a vote reward"
                                 :content (templates/vote-reward-claimed-email-body {:amount amount
                                                                                     :title title})
                                 :substitutions {:header (str "Reward for voting on " title)}
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
                                 :print-mode? print-mode?})
                    (log/warn "No email found for voter" {:event ev :meme meme} ::send-vote-reward-claimed-email)))))))

(defn send-challenge-reward-claimed-email [{:keys [:registry-entry :timestamp :version :challenger :amount] :as ev}]
  (try-catch
   (let [{:keys [:meme/title] :as meme} (db/get-meme registry-entry)
         {:keys [:from :template-id :api-key :print-mode?]} @emailer]
     (promise-> (district0x-emails/get-email {:district0x-emails/address challenger})
                #(validate-email %)
                (fn [to] (if to
                           (send-email {:from from
                                        :to to
                                        :subject "You received a challenge reward"
                                        :content (templates/challenge-reward-claimed-email-body {:amount amount
                                                                                                 :title title})
                                        :substitutions {:header (str "Reward for challenging " registry-entry)}
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
                                        :print-mode? print-mode?})
                           (log/warn "No email found for challenger" {:event ev :meme meme} ::send-challenge-reward-claimed-email)))))))

(defn event-callback [f]
  (fn [error res]
    (if error
      (log/error "Emailer got error from blockchain event" {:error error} ::event-callback)
      (f (:args res)))))

(defn start [{:keys [:api-key :private-key :print-mode?] :as opts}]
  (when-not private-key
    (throw (js/Error. ":private-key is required to start emailer")))
  (merge opts
         {:listeners
          [(registry/challenge-created-event [:meme-registry :meme-registry-fwd] "latest" (event-callback send-challenge-created-email))
           (meme-auction-factory/meme-auction-buy-event "latest" (event-callback send-auction-bought-email))
           (registry/challenge-reward-claimed-event [:meme-registry :meme-registry-fwd] "latest" (event-callback send-challenge-reward-claimed-email))
           (registry/vote-reward-claimed-event [:meme-registry :meme-registry-fwd] "latest" (event-callback send-vote-reward-claimed-email))]}))

(defn stop [emailer]
  (doseq [listener (remove nil? (:listeners @emailer))]
    (web3-eth/stop-watching! listener (fn [err]))))
