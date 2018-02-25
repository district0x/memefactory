(ns memefactory.shared.contract.param-change
  (:require
    [bignumber.core :as bn]
    [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def load-param-change-keys [:param-change/db
                             :param-change/key
                             :param-change/value-type
                             :param-change/value
                             :param-change/applied-on])

(defn parse-load-param-change [param-change & [{:keys [:parse-dates?]}]]
  (when param-change
    (let [param-change (zipmap load-param-change-keys param-change)]
      (-> param-change
        (update :param-change/value bn/number)
        (update :param-change/applied-on (if parse-dates? web3-time->local-date-time bn/number))))))
