(ns memefactory.tests.smart-contracts.meme-tests
  (:require
    [bignumber.core :as bn]
    [cljs-time.coerce :refer [to-epoch from-long]]
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.evm :as web3-evm]
    [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
    [district.server.config]
    [district.server.smart-contracts :refer [contract-address]]
    [district.server.web3 :refer [web3]]
    [memefactory.server.contract.meme :as meme]
    [memefactory.server.contract.meme-factory :as meme-factory]
    [memefactory.server.contract.param-change :as param-change]
    [memefactory.server.contract.param-change-factory :as param-change-factory]
    [memefactory.server.contract.registry :as registry]
    [memefactory.server.contract.registry-entry :as registry-entry]
    [memefactory.server.deployer]
    [memefactory.shared.smart-contracts :refer [smart-contracts]]
    [memefactory.tests.smart-contracts.utils :refer [now create-before-fixture after-fixture]]
    [print.foo :include-macros true]
    [memefactory.server.contract.ds-auth :as ds-auth]
    [memefactory.server.contract.minime-token :as minime-token]
    [memefactory.server.contract.ds-guard :as ds-guard]
    [memefactory.server.contract.dank-token :as dank-token]))


(use-fixtures
  :each {:before (create-before-fixture)
         :after after-fixture})


(deftest tests
  (let [[addr0] (web3-eth/accounts @web3)]
    (is (= 1 1))))