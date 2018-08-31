(ns memefactory.tests.smart-contracts.deployment-tests
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
    [memefactory.server.contract.dank-token :as dank-token]
    [district.web3-utils :as web3-utils]))


(use-fixtures
  :once {:before (create-before-fixture)
         :after after-fixture})


(deftest deployment-tests
  (let [[addr0] (web3-eth/accounts @web3)]
    (is (= (ds-auth/authority :ds-guard) (contract-address :ds-guard)))
    (is (bn/= (dank-token/total-supply) (web3/to-wei 1000000000 :ether)))
    (is (web3-utils/zero-address? (dank-token/controller)))
    (is (= (ds-auth/authority :meme-registry-db) (contract-address :ds-guard)))
    (is (= (ds-auth/authority :param-change-registry-db) (contract-address :ds-guard)))
    (is (true? (ds-guard/can-call? {:src (contract-address :meme-registry-fwd)
                                    :dst (contract-address :meme-registry-db)
                                    :sig ds-guard/ANY})))
    (is (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                             :dst (contract-address :meme-registry-db)
                             :sig ds-guard/ANY}))
    (is (ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                             :dst (contract-address :param-change-registry-db)
                             :sig ds-guard/ANY}))
    (is (false? (ds-guard/can-call? {:src (contract-address :meme-registry-fwd)
                                     :dst (contract-address :param-change-registry-db)
                                     :sig ds-guard/ANY})))
    (is (registry/factory? [:meme-registry :meme-registry-fwd] (contract-address :meme-factory)))
    (is (registry/factory? [:param-change-registry :param-change-registry-fwd] (contract-address :param-change-factory)))

    (is (bn/> (dank-token/balance-of addr0) 0))))
