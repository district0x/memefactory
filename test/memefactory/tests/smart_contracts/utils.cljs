(ns memefactory.tests.smart-contracts.utils)

(defn tx-reverted? [tx-receipt]
  (nil? tx-receipt))
