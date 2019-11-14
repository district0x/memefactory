(ns memefactory.server.contract.meme
  (:require [bignumber.core :as bn]
            [cljs-web3.utils :as web3-utils]
            [district.server.smart-contracts :as smart-contracts]
            [district.shared.async-helpers :refer [promise->]]
            [memefactory.shared.utils :as shared-utils]
            [district.server.web3 :refer [web3]]))

;; (def load-meme-keys [:meme/meta-hash
;;                      :meme/total-supply
;;                      :meme/total-minted
;;                      :meme/token-id-start])

;; (defn parse-load-meme [contract-addr meme & [{:keys [:parse-dates?]}]]
;;   (when meme
;;     (let [meme (zipmap load-meme-keys meme)]
;;       (-> meme
;;           (assoc :reg-entry/address contract-addr)
;;           (update :meme/meta-hash web3-utils/to-ascii)
;;           (update :meme/total-supply bn/number)
;;           (update :meme/total-minted bn/number)
;;           (update :meme/token-id-start (constantly (shared-utils/parse-uint (:meme/token-id-start meme)))
;;                   #_bn/number)))))

(defn mint [contract-addr & [amount opts]]
  (smart-contracts/contract-send [:meme contract-addr] :mint [(or amount 0)] (merge {:gas 6000000} opts)))

(defn load-meme [contract-addr]
  (promise-> (smart-contracts/contract-call (smart-contracts/instance :meme contract-addr) :load-meme)
             (fn [meme]
               {:reg-entry/address contract-addr
                :meme/meta-hash (web3-utils/to-ascii @web3 (aget meme "0"))
                :meme/total-supply (bn/number (aget meme "1"))
                :meme/total-minted (bn/number (aget meme "2"))
                :meme/token-id-start (aget meme "3")})))

(defn transfer-deposit [contract-addr & [opts]]
  (smart-contracts/contract-send (smart-contracts/instance :meme contract-addr) :transfer-deposit [] (merge {:gas 300000} opts)))
