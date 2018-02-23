(ns memefactory.server.deployer
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.cljs-utils :refer [rand-str]]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-address deploy-smart-contract! write-smart-contracts!]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.eternal-db :as eternal-db]
    [memefactory.server.contract.dank-token :as dank-token]
    [memefactory.server.contract.ownable :as ownable]
    [memefactory.server.contract.registry :as registry]
    [mount.core :as mount :refer [defstate]]))

(declare deploy)
(defstate ^{:on-reload :noop} deployer
  :start (deploy (merge (:deployer @config)
                        (:deployer (mount/args)))))

(def registry-placeholder "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed")
(def dank-token-placeholder "deaddeaddeaddeaddeaddeaddeaddeaddeaddead")
(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")

(defn deploy-dank-token! [default-opts]
  (deploy-smart-contract! :DANK (merge default-opts {:gas 2200000
                                                     :arguments [(contract-address :minime-token-factory)
                                                                 (web3/to-wei 1000000000 :ether)]})))

(defn deploy-minime-token-factory! [default-opts]
  (deploy-smart-contract! :minime-token-factory (merge default-opts {:gas 2300000})))

(defn deploy-meme-registry-db! [default-opts]
  (deploy-smart-contract! :meme-registry-db (merge default-opts {:gas 1700000})))

(defn deploy-parameter-registry-db! [default-opts]
  (deploy-smart-contract! :parameter-registry-db (merge default-opts {:gas 1700000})))


(defn deploy-meme-registry! [default-opts]
  (deploy-smart-contract! :meme-registry (merge default-opts {:gas 1000000})))

(defn deploy-parameter-registry! [default-opts]
  (deploy-smart-contract! :parameter-registry (merge default-opts {:gas 1200000})))

(defn deploy-meme-registry-fwd! [default-opts]
  (deploy-smart-contract! :meme-registry-fwd (merge default-opts {:gas 500000
                                                                  :placeholder-replacements
                                                                  {forwarder-target-placeholder :meme-registry}})))

(defn deploy-parameter-registry-fwd! [default-opts]
  (deploy-smart-contract! :parameter-registry-fwd (merge default-opts
                                                         {:gas 500000
                                                          :placeholder-replacements
                                                          {forwarder-target-placeholder :parameter-registry}})))

(defn deploy-meme-token! [default-opts]
  (deploy-smart-contract! :meme-token (merge default-opts {:gas 1300000})))

(defn deploy-meme! [default-opts]
  (deploy-smart-contract! :meme (merge default-opts {:gas 3200000
                                                     :placeholder-replacements
                                                     {dank-token-placeholder :DANK
                                                      registry-placeholder :meme-registry-fwd
                                                      forwarder-target-placeholder :meme-token}})))

(defn deploy-parameter-change! [default-opts]
  (deploy-smart-contract! :parameter-change (merge default-opts {:gas 2500000
                                                                 :placeholder-replacements
                                                                 {dank-token-placeholder :DANK
                                                                  registry-placeholder :meme-registry-fwd}})))


(defn deploy-meme-factory! [default-opts]
  (deploy-smart-contract! :meme-factory (merge default-opts {:gas 1000000
                                                             :arguments [(contract-address :meme-registry-fwd)
                                                                         (contract-address :DANK)]
                                                             :placeholder-replacements
                                                             {forwarder-target-placeholder :meme}})))

(defn deploy-parameter-change-factory! [default-opts]
  (deploy-smart-contract! :parameter-change-factory (merge default-opts {:gas 1000000
                                                                         :arguments [(contract-address :parameter-registry-fwd)]
                                                                         :placeholder-replacements
                                                                         {forwarder-target-placeholder :parameter-change}})))


(defn deploy [{:keys [:write? :initial-registry-params :transfer-dank-token-to-accounts]
               :as deploy-opts}]
  (let [accounts (web3-eth/accounts @web3)
        deploy-opts (merge {:from (first accounts)} deploy-opts)]
    (deploy-minime-token-factory! deploy-opts)
    (deploy-dank-token! deploy-opts)
    (deploy-meme-registry-db! deploy-opts)
    (deploy-parameter-registry-db! deploy-opts)

    (deploy-meme-registry! deploy-opts)
    (deploy-parameter-registry! deploy-opts)

    (deploy-meme-registry-fwd! deploy-opts)
    (deploy-parameter-registry-fwd! deploy-opts)

    (registry/construct [:meme-registry :meme-registry-fwd]
                        {:db (contract-address :meme-registry-db)})

    (registry/construct [:parameter-registry :parameter-registry-fwd]
                        {:db (contract-address :parameter-registry-db)})

    (deploy-meme-token! deploy-opts)

    (deploy-meme! deploy-opts)
    (deploy-parameter-change! deploy-opts)

    (deploy-meme-factory! deploy-opts)
    (deploy-parameter-change-factory! deploy-opts)

    (eternal-db/set-uint-values :meme-registry-db (:meme-registry initial-registry-params))
    (eternal-db/set-uint-values :parameter-registry-db (:parameter-registry initial-registry-params))

    (ownable/transfer-ownership :meme-registry-db {:new-owner (contract-address :meme-registry-fwd)})
    (ownable/transfer-ownership :parameter-registry-db {:new-owner (contract-address :parameter-registry-fwd)})

    (registry/set-factory [:meme-registry :meme-registry-fwd]
                          {:factory (contract-address :meme-factory) :factory? true})

    (registry/set-factory [:parameter-registry :parameter-registry-fwd]
                          {:factory (contract-address :parameter-factory) :factory? true})

    (when (pos? transfer-dank-token-to-accounts)
      (doseq [account (take transfer-dank-token-to-accounts (rest accounts))]
        (dank-token/transfer {:to account :amount (web3/to-wei 15000 :ether)}
                             {:from (first accounts)})))

    (when write?
      (write-smart-contracts!))))

