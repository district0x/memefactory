(ns memefactory.server.deployer
  (:require
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.cljs-utils :refer [rand-str]]
    [district.server.config :refer [config]]
    [district.server.smart-contracts :refer [contract-event-in-tx contract-address deploy-smart-contract! write-smart-contracts!]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.dank-token :as dank-token]
    [memefactory.server.contract.ds-auth :as ds-auth]
    [memefactory.server.contract.ds-guard :as ds-guard]
    [memefactory.server.contract.eternal-db :as eternal-db]
    [memefactory.server.contract.meme-auction-factory :as meme-auction-factory]
    [memefactory.server.contract.mutable-forwarder :as mutable-forwarder]
    [memefactory.server.contract.registry :as registry]
    [district.shared.error-handling :as macros :refer [try-catch-throw]]
    [mount.core :as mount :refer [defstate]]))

(declare deploy)
(defstate ^{:on-reload :noop} deployer
  :start (deploy (merge (:deployer @config)
                        (:deployer (mount/args)))))

(def registry-placeholder "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed")
(def dank-token-placeholder "deaddeaddeaddeaddeaddeaddeaddeaddeaddead")
(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")
(def district-config-placeholder "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
(def meme-token-placeholder "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")
(def meme-auction-factory-placeholder "daffdaffdaffdaffdaffdaffdaffdaffdaffdaff")

(defn deploy-dank-token! [default-opts]
  (deploy-smart-contract! :DANK (merge default-opts {:gas 2200000
                                                     :arguments [(contract-address :minime-token-factory)
                                                                 (web3/to-wei 1000000000 :ether)]})))

(defn deploy-meme-token! [default-opts]
  (deploy-smart-contract! :meme-token (merge default-opts {:gas 2200000
                                                           :arguments [(contract-address :meme-registry-fwd)]})))

(defn deploy-minime-token-factory! [default-opts]
  (deploy-smart-contract! :minime-token-factory (merge default-opts {:gas 2300000})))

(defn deploy-ds-guard! [default-opts]
  (deploy-smart-contract! :ds-guard (merge default-opts {:gas 1000000})))

(defn deploy-district-config! [{:keys [:deposit-collector :meme-auction-cut-collector :meme-auction-cut] :as default-opts}]
  (deploy-smart-contract! :district-config (merge default-opts {:gas 1000000
                                                                :arguments [deposit-collector
                                                                            meme-auction-cut-collector
                                                                            meme-auction-cut]})))

(defn deploy-meme-registry-db! [default-opts]
  (deploy-smart-contract! :meme-registry-db (merge default-opts {:gas #_1700000 2000000})))

(defn deploy-param-change-registry-db! [default-opts]
  (deploy-smart-contract! :param-change-registry-db (merge default-opts {:gas #_1700000 2000000})))


(defn deploy-meme-registry! [default-opts]
  (deploy-smart-contract! :meme-registry (merge default-opts {:gas 1500000 #_1000000})))

(defn deploy-param-change-registry! [default-opts]
  (deploy-smart-contract! :param-change-registry (merge default-opts {:gas 1700000})))

(defn deploy-meme-registry-fwd! [default-opts]
  (deploy-smart-contract! :meme-registry-fwd (merge default-opts {:gas 500000
                                                                  :placeholder-replacements
                                                                  {forwarder-target-placeholder :meme-registry}})))

(defn deploy-param-change-registry-fwd! [default-opts]
  (deploy-smart-contract! :param-change-registry-fwd (merge default-opts
                                                            {:gas 500000
                                                             :placeholder-replacements
                                                             {forwarder-target-placeholder :param-change-registry}})))

(defn deploy-meme! [default-opts]
  (try-catch-throw
   (deploy-smart-contract! :meme (merge default-opts {:gas 6721975
                                                      :placeholder-replacements
                                                      {dank-token-placeholder :DANK
                                                       registry-placeholder :meme-registry-fwd
                                                       district-config-placeholder :district-config
                                                       meme-token-placeholder :meme-token}}))))

(defn deploy-param-change! [default-opts]
  (try-catch-throw
   (deploy-smart-contract! :param-change (merge default-opts {:gas 6000000
                                                              :placeholder-replacements
                                                              {dank-token-placeholder :DANK
                                                               registry-placeholder :param-change-registry-fwd}}))))


(defn deploy-meme-factory! [default-opts]
  (deploy-smart-contract! :meme-factory (merge default-opts {:gas 1000000
                                                             :arguments [(contract-address :meme-registry-fwd)
                                                                         (contract-address :DANK)
                                                                         (contract-address :meme-token)]
                                                             :placeholder-replacements
                                                             {forwarder-target-placeholder :meme}})))

(defn deploy-param-change-factory! [default-opts]
  (deploy-smart-contract! :param-change-factory (merge default-opts {:gas 1000000
                                                                     :arguments [(contract-address :param-change-registry-fwd)
                                                                                 (contract-address :DANK)]
                                                                     :placeholder-replacements
                                                                     {forwarder-target-placeholder :param-change}})))

(defn deploy-meme-auction-factory! [default-opts]
  (deploy-smart-contract! :meme-auction-factory (merge default-opts {:gas 2000000
                                                                     :placeholder-replacements
                                                                     {forwarder-target-placeholder :meme-auction}})))


(defn deploy-meme-auction-factory-fwd! [default-opts]
  (deploy-smart-contract! :meme-auction-factory-fwd (merge default-opts
                                                           {:gas 500000
                                                            :placeholder-replacements
                                                            {forwarder-target-placeholder :meme-auction-factory}})))

(defn deploy-meme-auction! [{:keys [:cut-collector] :as default-opts}]
  (deploy-smart-contract! :meme-auction (merge default-opts {:gas 4000000
                                                             :placeholder-replacements
                                                             {meme-auction-factory-placeholder :meme-auction-factory-fwd
                                                              registry-placeholder :meme-registry-fwd
                                                              district-config-placeholder :district-config
                                                              meme-token-placeholder :meme-token}})))

(defn deploy [{:keys [:write? :initial-registry-params :transfer-dank-token-to-accounts
                      :use-n-account-as-deposit-collector :use-n-account-as-cut-collector]
               :as deploy-opts}]
  (let [accounts (web3-eth/accounts @web3)
        deploy-opts (merge {:from (last accounts)
                            ;; this keys are to make testing simpler
                            :deposit-collector (nth accounts (or use-n-account-as-deposit-collector 0))
                            :meme-auction-cut-collector (nth accounts (or use-n-account-as-cut-collector 0))
                            :meme-auction-cut 0}
                           deploy-opts)]
    (deploy-ds-guard! deploy-opts)
    ;; make deployed :ds-guard its own autority
    (ds-auth/set-authority :ds-guard (contract-address :ds-guard) deploy-opts)

    (deploy-minime-token-factory! deploy-opts)
    (deploy-dank-token! deploy-opts)
    (deploy-district-config! deploy-opts)
    (ds-auth/set-authority :district-config (contract-address :ds-guard) deploy-opts)

    (deploy-meme-registry-db! deploy-opts)
    (deploy-param-change-registry-db! deploy-opts)

    (deploy-meme-registry! deploy-opts)
    (deploy-param-change-registry! deploy-opts)

    (deploy-meme-registry-fwd! deploy-opts)

    (deploy-param-change-registry-fwd! deploy-opts)

    (registry/construct [:meme-registry :meme-registry-fwd]
                        {:db (contract-address :meme-registry-db)}
                        deploy-opts)

    (registry/construct [:param-change-registry :param-change-registry-fwd]
                        {:db (contract-address :param-change-registry-db)}
                        deploy-opts)

    ;; Allow :param-change-registry-fwd to grand permissions to other contracts (for ParamChanges to apply changes)
    (ds-guard/permit {:src (contract-address :param-change-registry-fwd)
                      :dst (contract-address :ds-guard)
                      :sig ds-guard/ANY}
                     deploy-opts)

    (deploy-meme-token! deploy-opts)

    (deploy-meme! deploy-opts)
    (deploy-param-change! deploy-opts)

    (deploy-meme-factory! deploy-opts)

    (deploy-param-change-factory! deploy-opts)

    (eternal-db/set-uint-values :meme-registry-db (:meme-registry initial-registry-params) deploy-opts)
    (eternal-db/set-uint-values :param-change-registry-db (:param-change-registry initial-registry-params) deploy-opts)

    ;; make :ds-guard authority of both :meme-registry-db and :param-change-registry-db
    (ds-auth/set-authority :meme-registry-db (contract-address :ds-guard) deploy-opts)
    (ds-auth/set-authority :param-change-registry-db (contract-address :ds-guard) deploy-opts)
    ;; After authority is set, we can clean owner. Not really essential, but extra safety measure
    (ds-auth/set-owner :meme-registry-db 0 deploy-opts)
    (ds-auth/set-owner :param-change-registry-db 0 deploy-opts)

    ;; Allow :meme-registry-fwd to make changes into :meme-registry-db
    (ds-guard/permit {:src (contract-address :meme-registry-fwd)
                      :dst (contract-address :meme-registry-db)
                      :sig ds-guard/ANY}
                     deploy-opts)

    ;; Allow :param-change-registry-fwd to make changes into :meme-registry-db (to apply ParamChanges)
    (ds-guard/permit {:src (contract-address :param-change-registry-fwd)
                      :dst (contract-address :meme-registry-db)
                      :sig ds-guard/ANY}
                     deploy-opts)

    ;; Allow :param-change-registry-fwd to make changes into :param-change-registry-db
    (ds-guard/permit {:src (contract-address :param-change-registry-fwd)
                      :dst (contract-address :param-change-registry-db)
                      :sig ds-guard/ANY}
                     deploy-opts)

    (registry/set-factory [:meme-registry :meme-registry-fwd]
                          {:factory (contract-address :meme-factory) :factory? true}
                          deploy-opts)

    (registry/set-factory [:param-change-registry :param-change-registry-fwd]
                          {:factory (contract-address :param-change-factory) :factory? true}
                          deploy-opts)

    (deploy-meme-auction-factory-fwd! deploy-opts)
    (ds-auth/set-authority :meme-auction-factory-fwd (contract-address :ds-guard) deploy-opts)
    (deploy-meme-auction! deploy-opts)
    (deploy-meme-auction-factory! deploy-opts)
    (mutable-forwarder/set-target :meme-auction-factory-fwd (contract-address :meme-auction-factory) deploy-opts)
    (mutable-forwarder/target :meme-auction-factory-fwd)
    (meme-auction-factory/construct {:meme-token (contract-address :meme-token)} deploy-opts)

    (when (pos? transfer-dank-token-to-accounts)
      (doseq [account (take transfer-dank-token-to-accounts accounts)]
        (dank-token/transfer {:to account :amount (web3/to-wei 15000 :ether)}
                             ;; this is the deployer of dank-token so it owns the initial amount
                             {:from (last accounts)})))

    (when write?
      (write-smart-contracts!))))
