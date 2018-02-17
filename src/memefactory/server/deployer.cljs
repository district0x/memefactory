(ns memefactory.server.deployer
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-address deploy-smart-contract! write-smart-contracts!]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.ownable :as ownable]
    [memefactory.server.contract.registry :as registry]
    [mount.core :as mount :refer [defstate]]))

(declare deploy)
(defstate ^{:on-reload :noop} deployer
  :start (deploy (merge (:deployer @config)
                        (:deployer (mount/args)))))

(def registry-placeholder "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed")
(def MFM-placeholder "deaddeaddeaddeaddeaddeaddeaddeaddeaddead")
(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")

(defn deploy-MFM! [{:keys [:minime-factory-address] :as default-opts
                    :or {minime-factory-address 0}}]
  (deploy-smart-contract! :MFM (merge default-opts {:gas 2200000
                                                    :arguments [minime-factory-address

                                                                (web3/to-wei 1000000000 :ether)]})))

(defn deploy-meme-registry-db! [default-opts]
  (deploy-smart-contract! :meme-registry-db (merge default-opts {:gas 1300000})))

(defn deploy-parameter-registry-db! [default-opts]
  (deploy-smart-contract! :parameter-registry-db (merge default-opts {:gas 1300000})))


(defn deploy-meme-registry! [default-opts]
  (deploy-smart-contract! :meme-registry (merge default-opts {:gas 1000000
                                                              :arguments [(contract-address :meme-registry-db)]})))

(defn deploy-parameter-registry! [default-opts]
  (deploy-smart-contract! :parameter-registry (merge default-opts {:gas 1000000
                                                                   :arguments [(contract-address :parameter-registry-db)]})))

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
                                                     {MFM-placeholder :MFM
                                                      registry-placeholder :meme-registry
                                                      forwarder-target-placeholder :meme-token}})))

(defn deploy-parameter-change! [default-opts]
  (deploy-smart-contract! :parameter-change (merge default-opts {:gas 2500000
                                                                 :placeholder-replacements
                                                                 {MFM-placeholder :MFM
                                                                  registry-placeholder :meme-registry}})))


(defn deploy-meme-factory! [default-opts]
  (deploy-smart-contract! :meme-factory (merge default-opts {:gas 700000
                                                             :arguments [(contract-address :meme-registry)]
                                                             :placeholder-replacements
                                                             {forwarder-target-placeholder :meme}})))

(defn deploy-parameter-change-factory! [default-opts]
  (deploy-smart-contract! :parameter-change-factory (merge default-opts {:gas 700000
                                                                         :arguments [(contract-address :parameter-registry)]
                                                                         :placeholder-replacements
                                                                         {forwarder-target-placeholder :parameter-change}})))


(defn deploy [{:keys [:write? :emergency-multisig]
               :as deploy-opts}]
  (let [deploy-opts (merge {:from (first (web3-eth/accounts @web3))} deploy-opts)]
    (deploy-MFM! deploy-opts)
    (deploy-meme-registry-db! deploy-opts)
    (deploy-parameter-registry-db! deploy-opts)

    (deploy-meme-registry! deploy-opts)
    (deploy-parameter-registry! deploy-opts)

    (deploy-meme-registry-fwd! deploy-opts)
    (deploy-parameter-registry-fwd! deploy-opts)

    (deploy-meme-token! deploy-opts)

    (deploy-meme! deploy-opts)
    (deploy-parameter-change! deploy-opts)

    (deploy-meme-factory! deploy-opts)
    (deploy-parameter-change-factory! deploy-opts)

    (ownable/transfer-ownership :meme-registry-db {:new-owner (contract-address :meme-registry-fwd)})
    (print.foo/look (ownable/owner [:meme-registry (contract-address :meme-registry-fwd)]))
    (print.foo/look (ownable/owner :meme-registry))
    (print.foo/look (ownable/owner :meme-registry-db))
    (print.foo/look (registry/db [:meme-registry (contract-address :meme-registry-fwd)]))
    (print.foo/look (registry/db :meme-registry))

    (print.foo/look (ownable/owner [:parameter-registry (contract-address :parameter-registry-fwd)]))
    (print.foo/look (ownable/owner :parameter-registry))
    (print.foo/look (ownable/owner :parameter-registry-db))
    (print.foo/look (registry/db [:parameter-registry (contract-address :parameter-registry-fwd)]))
    (print.foo/look (registry/db :parameter-registry))

    (registry/set-factory #_ :meme-registry [:meme-registry (contract-address :meme-registry-fwd)]
                          {:factory (contract-address :meme-factory) :factory? true})

    #_(when write?
        (write-smart-contracts!))))

