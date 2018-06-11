(ns memefactory.shared.utils)

(defn calculate-meme-auction-price [#:meme-auction{:keys [:starting-price :end-price :duration :started-on]} now]
  (let [seconds-passed (- now started-on)
        total-price-change (- end-price starting-price)
        current-price-change (quot (* total-price-change seconds-passed) duration)]
    (if (>= seconds-passed duration)
      end-price
      (+ starting-price current-price-change))))
