(ns memefactory.shared.utils
  (:require [print.foo :refer [look] :include-macros true]
            [bignumber.core :as bn]))

(def not-nil? (complement nil?))

(defn calculate-meme-auction-price [{:keys [:meme-auction/start-price
                                            :meme-auction/end-price
                                            :meme-auction/duration
                                            :meme-auction/started-on] :as auction} now]
  (let [seconds-passed (- now started-on)
        total-price-change (- end-price start-price)
        current-price-change (/ (* total-price-change seconds-passed) duration)]
    (if (<= duration seconds-passed)
      end-price
      (+ start-price current-price-change))))
