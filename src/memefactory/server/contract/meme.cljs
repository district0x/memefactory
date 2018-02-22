(ns memefactory.server.contract.meme
  (:require
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.eth :as web3-eth]
    [district.server.smart-contracts :refer [contract-call instance contract-address]]
    [memefactory.server.contract.dank-token :as mfm]))

