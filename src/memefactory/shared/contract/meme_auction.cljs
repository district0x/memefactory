(ns memefactory.shared.contract.meme-auction
  (:require
    [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]
    [bignumber.core :as bn]))

(def load-meme-auction-keys [:meme-auction/seller
                             :meme-auction/token-id
                             :meme-auction/start-price
                             :meme-auction/end-price
                             :meme-auction/duration
                             :meme-auction/started-on
                             :reg-entry/address
                             :meme-auction/description])

(defn parse-load-meme-auction [contract-addr meme & [{:keys [:parse-dates?]}]]
  (when meme
    (let [meme (zipmap load-meme-auction-keys meme)]
      (-> meme
        (assoc :meme-auction/address contract-addr)
        (update :meme-auction/token-id bn/number)
        (update :meme-auction/start-price bn/number)
        (update :meme-auction/end-price bn/number)
        (update :meme-auction/duration bn/number)
        (update :meme-auction/started-on (if parse-dates? web3-time->local-date-time bn/number))))))
