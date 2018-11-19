(ns memefactory.server.emailer
  (:require
    [cljs-web3.eth :as web3-eth]
    [district.encryption :as encryption]
    [district.sendgrid :refer [send-email]]
    [district.server.config :refer [config]]
    [goog.format.EmailAddress :as email-address]
    [mount.core :as mount :refer [defstate]]
    [memefactory.server.deployer]
    [memefactory.server.generator]
    [taoensso.timbre :refer [info warn error]]
    [memefactory.server.contract.district0x-emails :refer [get-email]]
    [memefactory.server.emailer.templates :as templates]
    [memefactory.server.db :as mf-db]
    [memefactory.server.contract.registry :as registry]
    [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]))

(declare start)
(declare stop)
(defstate emailer
  :start (start (merge (:emailer @config)
                       (:emailer (mount/args))))
  :stop (stop emailer))

(def template-id "")
(def from "district0x@district0x.io")

(defn validate-email [base64-encrypted-email]
  (when-not (empty? base64-encrypted-email)
    (let [email (encryption/decode-decrypt (:private-key @emailer) base64-encrypted-email)]
      (when (email-address/isValidAddress email)
        email))))

(defn send-challenge-created-email [{:keys [:registry-entry :challenger :commit-period-end
                                            :reveal-period-end :reward-pool :metahash :timestamp :version] :as ev}]
  (try
    (let [meme (mf-db/get-meme registry-entry)
          owner-encrypted-email (get-email {:district0x-emails/address (:reg-entry/creator meme)})]
      (when-let [to (validate-email owner-encrypted-email)]
        (send-email {:from from
                     :to to
                     :subject (str "Your meme has been challenged ")
                     :content (templates/challenge-created-email-body meme)
                     :substitutions {:header (str (:meme/title meme) " meme challenged")
                                     :button-title "See challenge details"
                                     :button-href ""}
                     :on-success #(info "Success sending email" {} ::send-challenge-created-email)
                     :on-error #(error "Error when sending email" {:error %} ::send-challenge-created-email)
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})))))

(defn send-auction-bought-email [to {:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds]}]
  (try
    (let [meme-auction (mf-db/get-meme-auction meme-auction)
          encrypted-email (get-email {:district0x-emails/address (:meme-auction/seller meme-auction)})]
      (when-let [to (validate-email encrypted-email)]
        (send-email {:from from
                     :to to
                     :subject (str "One of your auctions has been bought")
                     :content (templates/meme-auction-bought-email-body meme-auction)
                     :substitutions {:header (str "Your auction %s has been bought " meme-auction)
                                     :button-title "See details"
                                     :button-href ""}
                     :on-success #(info "Success sending email" {} ::send-auction-bought-email)
                     :on-error #(error "Error when sending email" {:error %} ::send-auction-bought-email)
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})))))

(defn send-vote-reward-claimed-email [to {:keys [:registry-entry :timestamp :version :voter :amount]}]
  (try
    (let [encrypted-email (get-email {:district0x-emails/address voter})]
      (when-let [to (validate-email encrypted-email)]
        (send-email {:from from
                     :to to
                     :subject (str "Vote reward ")
                     :content (templates/vote-reward-claimed-email-body {:amount amount
                                                                         :registry-entry registry-entry})
                     :substitutions {:header (str "Reward for voting on " registry-entry)}
                     :on-success #(info "Success sending email" {} ::send-vote-reward-claimed-email)
                     :on-error #(error "Error when sending email" {:error %} ::send-vote-reward-claimed-email)
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})))))

(defn send-challenge-reward-claimed-email [to {:keys [:registry-entry :timestamp :version :challenger :amount]}]
  (try
    (let [encrypted-email (get-email {:district0x-emails/address challenger})]
      (when-let [to (validate-email encrypted-email)]
        (send-email {:from from
                     :to to
                     :subject (str "Challenge reward ")
                     :content (templates/challenge-reward-claimed-email-body {:amount amount
                                                                              :registry-entry registry-entry})
                     :substitutions {:header (str "Reward for challenging " registry-entry)}
                     :on-success #(info "Success sending email" {} ::send-challenge-reward-claimed-email)
                     :on-error #(error "Error when sending email" {:error %} ::send-challenge-reward-claimed-email)
                     :template-id template-id
                     :api-key (:api-key @emailer)
                     :print-mode? (:print-mode? @emailer)})))))


(defn event-callback [f]
  (fn [error res]
    (if error
      (error "Emailer got error from blockchain event" {:error error} ::event-callback)
      (f (:args res)))))


(defn start [{:keys [:api-key :private-key :print-mode?] :as opts}]
  (when-not private-key
    (throw (js/Error. ":private-key is required to start emailer")))
  (merge opts
         {:listeners
          [(registry/challenge-created-event [:meme-registry :meme-registry-fwd] {} "latest" (event-callback send-challenge-created-email))
           (meme-auction-factory/meme-auction-buy-event {} "latest" (event-callback send-auction-bought-email))
           (registry/challenge-reward-claimed-event [:meme-registry :meme-registry-fwd] {} "latest" (event-callback send-challenge-reward-claimed-email))
           (registry/vote-reward-claimed-event [:meme-registry :meme-registry-fwd] {} "latest" (event-callback send-vote-reward-claimed-email))]}))


(defn stop [emailer]
  (doseq [listener (remove nil? (:listeners @emailer))]
    (web3-eth/stop-watching! listener (fn [err]))))
