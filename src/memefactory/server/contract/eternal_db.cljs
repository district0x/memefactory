(ns memefactory.server.contract.eternal-db
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [district.server.smart-contracts :refer [contract-call]]))

(defn set-uint-values [contract-key m & [opts]]
  (let [records (->> (keys m)
                  (map cs/->camelCaseString)
                  (map solidity-sha3))]
    (contract-call contract-key :set-u-int-values records (vals m) (merge opts {:gas 500000}))))

(defn get-uint-value [contract-key kw]
  (contract-call contract-key :get-u-int-value (solidity-sha3 (cs/->camelCaseString kw))))

(defn get-uint-values [contract-key kws]
  (contract-call contract-key :get-u-int-values (->> kws
                                                  (map cs/->camelCaseString)
                                                  (map solidity-sha3))))

(defn change-applied-event [contract-key & args]
  (apply contract-call contract-key :EternalDbEvent args))
