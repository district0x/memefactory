(ns memefactory.shared.contract.param-change
  (:require [bignumber.core :as bn]
            [memefactory.shared.utils :as shared-utils]))

(def load-param-change-keys [:param-change/db
                             :param-change/key
                             :param-change/value])

(defn parse-load-param-change [contract-addr param-change & [{:keys [:parse-dates?]}]]
  (when param-change
    (let [param-change (zipmap load-param-change-keys param-change)]
      (-> param-change
          (assoc :reg-entry/address contract-addr)
          (update :param-change/value bn/number)))))
