(ns memefactory.server.emailer
  (:require
    [cljs-time.coerce :as time-coerce]
    [cljs-time.core :as t]
    [clojure.string :as string]
    [district.encryption :as encryption]
    [district.format :as format]
    [district.sendgrid :refer [send-email]]
    [district.server.config :as config]
    [district.server.logging]
    [district.server.web3-events :refer [register-callback! unregister-callbacks!]]
    [district.shared.async-helpers :refer [promise->]]
    [district.shared.error-handling :refer [try-catch try-catch-throw]]
    [district.time :as time]
    [goog.format.EmailAddress :as email-address]
    [memefactory.server.contract.district0x-emails :as district0x-emails]
    [memefactory.server.db :as db]
    [memefactory.server.emailer.templates :as templates]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))

(defn validate-email [base64-encrypted-email]
  (when-not (empty? base64-encrypted-email)
    (let [email (encryption/decode-decrypt (get-in @config/config [:emailer :private-key]) base64-encrypted-email)]
      (when (email-address/isValidAddress email)
        email))))

(defn email-supported-extension? [image-url]
  (cond
    (string/includes? image-url ".png") true
    (string/includes? image-url ".jpeg") true
    (string/includes? image-url ".jpg") true
    (string/includes? image-url ".gif") true
    :else false))

(defn send-challenge-created-email-handler
  [{:keys [from to
           title
           meme-url
           meme-image-url
           button-url
           time-remaining
           on-success on-error
           template-id
           api-key
           print-mode?]}]
  (send-email
   {:from from
    :to to
    :subject (str title " was challenged!")
    :content (templates/challenge-created-email-body {:meme/title title
                                                      :meme-url meme-url
                                                      :time-remaining time-remaining})
    :substitutions {:header (str title " was challenged")
                    :button-title "Vote Now"
                    :button-href button-url
                    :meme-image-url meme-image-url
                    :meme-image-class (if (email-supported-extension? meme-image-url) "show" "no-show")}
    :on-success on-success
    :on-error on-error
    :template-id template-id
    :api-key api-key
    :print-mode? print-mode?}))


(defn send-challenge-created-email [{:keys [:registry-entry :challenger :commit-period-end
                                            :reveal-period-end :reward-pool :metahash :timestamp :version] :as ev}]
  (let [{:keys [:reg-entry/creator :meme/title :meme/image-hash] :as meme} (db/get-meme registry-entry)
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))
        meme-url (str root-url "meme-detail/" registry-entry)
        [unit value] (time/time-remaining-biggest-unit (t/now)
                                                       (-> commit-period-end time/epoch->long time-coerce/from-long))
        time-remaining (format/format-time-units {unit value})
        meme-image-url (str ipfs-gateway-url image-hash)]
    (promise-> (district0x-emails/get-email {:district0x-emails/address creator})
               #(validate-email %)
               (fn [to] (if to
                          (do
                            (log/info "Sending meme challenged email" ev ::send-challenge-created-email)
                            (send-challenge-created-email-handler
                             {:from from
                              :to to
                              :title title
                              :meme-url meme-url
                              :meme-image-url meme-image-url
                              :button-url meme-url
                              :time-remaining time-remaining
                              :on-success #(log/info "Success sending meme challenged email"
                                                     {:to to :registry-entry registry-entry :meme-title title}
                                                     ::send-challenge-created-email)
                              :on-error #(log/error "Error when sending meme challenged email"
                                                    {:error % :event ev :meme meme :to to}
                                                    ::send-challenge-created-email)
                              :template-id template-id
                              :api-key api-key
                              :print-mode? print-mode?}))
                          (log/info "No email found for challenged meme creator" {:event ev :meme meme} ::send-challenge-created-email))))))

(defn send-auction-bought-email-handler
  [{:keys [from to title
           meme-url
           meme-image-url
           button-url
           buyer-address
           buyer-url
           price
           on-success on-error
           template-id
           api-key
           print-mode?]}]
  (send-email {:from from
               :to to
               :subject (str title " was sold!")
               :content (templates/meme-auction-bought-email-body {:meme/title title
                                                                   :meme-url meme-url
                                                                   :buyer-address buyer-address
                                                                   :buyer-url buyer-url
                                                                   :price price})
               :substitutions {:header (str title " was sold!")
                               :button-title "My Memefolio"
                               :button-href button-url
                               :meme-image-url meme-image-url
                               :meme-image-class (if (email-supported-extension? meme-image-url) "show" "no-show")}
               :on-success on-success
               :on-error on-error
               :template-id template-id
               :api-key api-key
               :print-mode? print-mode?}))

(defn send-auction-bought-email [{:keys [:meme-auction :timestamp :buyer :price :auctioneer-cut :seller-proceeds] :as ev}]
  (let [{:keys [:meme-auction/seller :meme-auction/address] :as meme-auction} (db/get-meme-auction meme-auction)
        {:keys [:reg-entry/address :meme/title :meme/image-hash]} (db/get-meme-by-auction-address address)
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))
        meme-image-url (str ipfs-gateway-url image-hash)
        buyer-url (str root-url "memefolio/" buyer)
        meme-url (str root-url "meme-detail/" address)
        button-url (str root-url "memefolio/?tab=sold")]
    (promise-> (district0x-emails/get-email {:district0x-emails/address seller})
               #(validate-email %)
               (fn [to]
                 (if to
                   (send-auction-bought-email-handler
                    {:from from
                     :to to
                     :title title
                     :meme-url meme-url
                     :meme-image-url meme-image-url
                     :buyer-address buyer
                     :buyer-url buyer-url
                     :price price
                     :button-url button-url
                     :on-success #(log/info "Success sending auction bought email"
                                            {:to to :meme-auction meme-auction :meme-title title}
                                            ::send-auction-bought-email)
                     :on-error #(log/error "Error when sending auction-bought email"
                                           {:error % :event ev :meme-auction meme-auction :to to}
                                           ::send-auction-bought-email)
                     :template-id template-id
                     :api-key api-key
                     :print-mode? print-mode?})
                   (log/info "No email found for meme auction seller" {:event ev :meme-auction meme-auction} ::send-auction-bought-email))))))

(defn send-vote-reward-claimed-email-handler
  [{:keys [to from
           title option
           amount
           meme-url
           meme-image-url
           button-url
           on-success
           on-error
           template-id
           api-key
           print-mode?]}]
  (send-email {:from from
               :to to
               :subject "You received a vote reward"
               :content (templates/vote-reward-claimed-email-body {:meme/title title
                                                                   :vote/option (case option
                                                                                  1 "DANK"
                                                                                  2 "STANK")
                                                                   :amount amount
                                                                   :meme-url meme-url})
               :substitutions {:header "Vote Reward"
                               :button-title "My Memefolio"
                               :button-href button-url
                               :meme-image-url meme-image-url
                               :meme-image-class (if (email-supported-extension? meme-image-url) "show" "no-show")}
               :on-success on-success
               :on-error on-error
               :template-id template-id
               :api-key api-key
               :print-mode? print-mode?}))

(defn send-vote-reward-claimed-email [{:keys [:registry-entry :timestamp :version :voter :amount] :as ev}]
  (let [{:keys [:meme/title :meme/image-hash] :as meme} (db/get-meme registry-entry)
        {:keys [:vote/option]} (db/get-vote {:reg-entry/address registry-entry :vote/voter voter} [:vote/option])
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer ])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))
        button-url (str root-url "memefolio/?tab=curated")
        meme-url (str root-url "meme-detail/" registry-entry)
        meme-image-url (str ipfs-gateway-url image-hash)]
    (promise-> (district0x-emails/get-email {:district0x-emails/address voter})
               #(validate-email %)
               (fn [to]
                 (if to
                   (do
                     (send-vote-reward-claimed-email-handler
                      {:to to
                       :from from
                       :title title
                       :option option
                       :amount amount
                       :meme-url meme-url
                       :meme-image-url meme-image-url
                       :button-url button-url
                       :on-success #(log/info "Success sending email"
                                              {:to to :registry-entry registry-entry :meme-title title}
                                              ::send-vote-reward-claimed-email)
                       :on-error #(log/error "Error when sending email"
                                             {:error % :event ev :meme meme :to to}
                                             ::send-vote-reward-claimed-email)
                       :template-id template-id
                       :api-key api-key
                       :print-mode? print-mode?})
                     (log/info "Sending vote reward received email" ev ::send-vote-reward-claimed-email))
                   (log/info "No email found for voter" {:event ev :meme meme} ::send-vote-reward-claimed-email))))))

(defn send-challenge-reward-claimed-email-handler
  [{:keys [to from
           title
           amount
           meme-url
           meme-image-url
           button-url
           on-success
           on-error
           template-id
           api-key
           print-mode?]}]
  (send-email {:from from
               :to to
               :subject "You received a challenge reward"
               :content (templates/challenge-reward-claimed-email-body {:amount amount
                                                                        :meme/title title
                                                                        :meme-url meme-url})
               :substitutions {:header "Challenge Reward"
                               :button-title "My Memefolio"
                               :button-href button-url
                               :meme-image-url meme-image-url
                               :meme-image-class (if (email-supported-extension? meme-image-url) "show" "no-show")}
               :on-success on-success
               :on-error on-error
               :template-id template-id
               :api-key api-key
               :print-mode? print-mode?}))

(defn send-challenge-reward-claimed-email [{:keys [:registry-entry :timestamp :version :challenger :amount] :as ev}]
  (let [{:keys [:meme/title :meme/image-hash] :as meme} (db/get-meme registry-entry)
        {:keys [:from :template-id :api-key :print-mode?]} (get-in @config/config [:emailer])
        root-url (format/ensure-trailing-slash (get-in @config/config [:ui :root-url]))
        ipfs-gateway-url (format/ensure-trailing-slash (get-in @config/config [:ipfs :gateway]))
        meme-url (str root-url "meme-detail/" registry-entry)
        meme-image-url (str ipfs-gateway-url image-hash)
        button-url (str root-url "memefolio/?tab=curated")]
    (promise-> (district0x-emails/get-email {:district0x-emails/address challenger})
               #(validate-email %)
               (fn [to]
                 (if to
                   (do
                     (log/info "Sending chalenge reward received email" ev ::send-challenge-reward-claimed-email)
                     (send-challenge-reward-claimed-email-handler
                      {:from from
                       :to to
                       :title title
                       :amount amount
                       :meme-url meme-url
                       :meme-image-url meme-image-url
                       :button-url button-url
                       :on-success #(log/info "Success sending challenge reward claimed email"
                                              {:to to :registry-entry registry-entry :title title}
                                              ::send-challenge-reward-claimed-email)
                       :on-error #(log/error "Error when sending challenge reward claimed email"
                                             {:error % :event ev :meme meme :to to}
                                             ::send-challenge-reward-claimed-email)
                       :template-id template-id
                       :api-key api-key
                       :print-mode? print-mode?}))
                   (log/info "No email found for challenger" {:event ev :meme meme} ::send-challenge-reward-claimed-email))))))


(defn- dispatcher [callback]
  (fn [_ {:keys [:latest-event? :args] :as ev}]
    (when latest-event?
      (callback args))))


(defn start [opts]
  (let [callback-ids
        [(register-callback! :meme-registry/challenge-created-event (dispatcher send-challenge-created-email))
         (register-callback! :meme-auction-factory/meme-auction-buy-event (dispatcher send-auction-bought-email))
         (register-callback! :meme-registry/vote-reward-claimed-event (dispatcher send-vote-reward-claimed-email))
         (register-callback! :meme-registry/challenge-reward-claimed-event (dispatcher send-challenge-reward-claimed-email))]]
    (assoc opts :callback-ids callback-ids)))


(defn stop [emailer]
  (unregister-callbacks! (:callback-ids @emailer)))


(defstate emailer
  :start (start (merge (:emailer @config/config)
                       (:emailer (mount/args))))
  :stop (stop emailer))
