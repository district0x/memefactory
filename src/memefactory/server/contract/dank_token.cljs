(ns memefactory.server.contract.dank-token
  (:require [memefactory.server.contract.minime-token :as minime-token]))

(def approve-and-call (partial minime-token/approve-and-call :DANK))

;; (def approve (partial minime-token/approve :DANK))
;; (def transfer (partial minime-token/transfer :DANK))
(def balance-of (partial minime-token/balance-of :DANK))
(def controller (partial minime-token/controller :DANK))
(def total-supply (partial minime-token/total-supply :DANK))
