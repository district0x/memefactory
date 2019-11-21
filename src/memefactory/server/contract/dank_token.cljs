(ns memefactory.server.contract.dank-token
  (:require [memefactory.server.contract.minime-token :as minime-token]))

(def approve-and-call (partial minime-token/approve-and-call :DANK))
(def balance-of (partial minime-token/balance-of :DANK))
(def controller (partial minime-token/controller :DANK))
(def total-supply (partial minime-token/total-supply :DANK))
