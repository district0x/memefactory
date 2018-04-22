(ns memefactory.shared.contract.meme
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def load-meme-keys [:meme/meta-hash
                     :meme/total-supply
                     :meme/total-minted
                     :meme/token-id-start])

(defn parse-load-meme [contract-addr meme & [{:keys [:parse-dates?]}]]
  (when meme
    (let [meme (zipmap load-meme-keys meme)]
      (-> meme
        (assoc :reg-entry/address contract-addr)
        (update :meme/meta-hash web3/to-ascii)
        (update :meme/total-supply bn/number)
        (update :meme/total-minted bn/number)
        (update :meme/token-id-start bn/number)))))
