(ns memefactory.shared.utils
  (:require [print.foo :refer [look] :include-macros true]
            [bignumber.core :as bn]))

(defn calculate-meme-auction-price [#:meme-auction{:keys [:starting-price :end-price :duration :started-on]} now]
  (let [seconds-passed (- now started-on)
        total-price-change (- end-price starting-price)
        current-price-change (/ (* total-price-change seconds-passed) duration)]
    (if (<= duration seconds-passed)
      end-price
      (+ starting-price current-price-change))))
