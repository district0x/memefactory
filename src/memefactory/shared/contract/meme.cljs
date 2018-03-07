(ns memefactory.shared.contract.meme
  (:require
    [bignumber.core :as bn]
    [cljs-web3.core :as web3]
    [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def load-meme-keys [:meme/offering-start-price
                     :meme/offering-duration
                     :meme/token
                     :meme/total-supply
                     :meme/offering-supply
                     :meme/meta-hash])

(defn parse-load-meme [contract-addr meme & [{:keys [:parse-dates?]}]]
  (when meme
    (let [meme (zipmap load-meme-keys meme)]
      (-> meme
        (assoc :reg-entry/address contract-addr)
        (update :meme/total-supply bn/number)
        (update :meme/meta-hash web3/to-ascii)
        (update :meme/offering-duration bn/number)
        (update :meme/offering-start-price bn/number)
        (update :meme/offering-supply bn/number)))))
