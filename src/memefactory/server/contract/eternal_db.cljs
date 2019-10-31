(ns memefactory.server.contract.eternal-db
  (:require [camel-snake-kebab.core :as camel-snake-kebab]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [cljs-web3.utils :as web3-utils]))

;; (defn set-uint-values [contract-key m & [opts]]
;;   (let [records (->> (keys m)
;;                      (map cs/->camelCaseString)
;;                      (map solidity-sha3))]
;;     (contract-call contract-key :set-u-int-values [records (vals m)] (merge opts {:gas 500000}))))

;; (defn get-uint-value [contract-key ]
;;   (contract-call contract-key :get-u-int-value [(solidity-sha3 (cs/->camelCaseString kw))]))

;; (smart-contracts/contract-call :my-contract :counter)

(defn get-uint-values [contract-key db-keys]
  (smart-contracts/contract-call contract-key
                                 :get-u-int-values
                                 [(->> db-keys
                                       (map camel-snake-kebab/->camelCaseString)
                                       (map #(web3-utils/solidity-sha3 @web3 %)))]))

;; (defn change-applied-event [contract-key opts on-event]
;;   (create-event-filter contract-key :EternalDbEvent {} opts on-event))
