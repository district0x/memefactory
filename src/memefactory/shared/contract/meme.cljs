(ns memefactory.shared.contract.meme
  (:require
    [bignumber.core :as bn]
    [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def load-meme-keys [:meme/start-price
                     :meme/sale-duration
                     :meme/token
                     :meme/image-hash
                     :meme/meta-hash])

(defn parse-load-meme [meme & [{:keys [:parse-dates?]}]]
  (when meme
    (let [meme (zipmap load-meme-keys meme)]
      (-> meme
        (update :meme/sale-duration bn/number)))))
