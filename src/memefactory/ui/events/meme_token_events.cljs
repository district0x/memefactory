(ns memefactory.ui.events.meme-token-events
  (:require
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs.spec.alpha :as s]
   [district.ui.logging.events :as logging]
   [district.ui.smart-contracts.queries :as contract-queries]
   [district.ui.web3-accounts.queries :as account-queries]
   [district.ui.web3-tx.events :as tx-events]
   [district0x.re-frame.spec-interceptors :as spec-interceptors]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [reg-event-fx]]
   ))

(def interceptors [re-frame/trim-v])

(reg-event-fx
  :meme-token/transfer-multi-and-start-auction
  [interceptors #_(validate-first-arg (s/keys :req []))]
  (fn [{:keys [:db]} [{:keys [:token-ids :meme-auction/start-price :meme-auction/end-price :meme-auction/duration :meme-auction/description] :as args}]]

    (prn :meme-token/transfer-multi-and-start-auction args)

    (let [active-account (account-queries/active-account db)]
      {:dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-token)
                                       :fn :safe-transfer-from-multi
                                       :args (look [ active-account
                                                    (contract-queries/contract-address db :meme-auction-factory-fwd)
                                                    #_token-ids [1]
                                                    (web3-eth/contract-get-data (contract-queries/instance db :meme-auction) :start-auction (web3/to-wei start-price :ether) (web3/to-wei end-price :ether) duration description)])
                                       :tx-opts {:from active-account
                                                 :gas 6000000}
                                       :on-tx-success [::logging/success [:meme-token/transfer-multi]]
                                       :on-tx-hash-error [::logging/error [:meme-token/transfer-multi-and-start-auction]]
                                       :on-tx-error [::logging/error [:meme-token/transfer-multi-and-start-auction]]}]})))


(comment

(web3-eth/contract-get-data (contract-queries/instance db :meme-auction) :start-auction 0 0 0 "bla")

  (let [meme-token-instance :todo
        active-account "0xcd497a9eee711fe3b0393d41350f408158ad9923"
        meme-auction-factory-fwd "0x71837e8653d3f246699466f5a29d13e5bd9181fb"
        token-ids [1]
        data "0x680ed24f00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000"]
    (re-frame/dispatch [::tx-events/send-tx {:instance (contract-queries/instance db :meme-token)
                                             :fn :safe-transfer-from-multi
                                             :args [ active-account
                                                    meme-auction-factory-fwd
                                                    token-ids
                                                    data]
                                             :tx-opts {:from active-account
                                                       :gas 6000000}
                                             :on-tx-success [::logging/success [:meme-token/transfer-multi]]
                                             :on-tx-hash-error [::logging/error [:meme-token/transfer-multi-and-start-auction]]
                                             :on-tx-error [::logging/error [:meme-token/transfer-multi-and-start-auction]]}]))

  )
