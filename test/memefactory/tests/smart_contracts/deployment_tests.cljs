(ns memefactory.tests.smart-contracts.deployment-tests
  (:require [bignumber.core :as bn]
            [cljs-time.coerce :refer [to-epoch from-long]]
            [cljs-time.core :as t]
            [cljs-web3.core :as web3]
            [cljs-web3.eth :as web3-eth]
            [cljs-web3.evm :as web3-evm]
            [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]
            [district.server.config]
            [district.server.smart-contracts :refer [contract-address]]
            [district.server.web3 :refer [web3]]
            [district.web3-utils :as web3-utils]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.contract.ds-auth :as ds-auth]
            [memefactory.server.contract.ds-guard :as ds-guard]
            [memefactory.server.contract.meme :as meme]
            [memefactory.server.contract.meme-factory :as meme-factory]
            [memefactory.server.contract.minime-token :as minime-token]
            [memefactory.server.contract.param-change :as param-change]
            [memefactory.server.contract.param-change-factory :as param-change-factory]
            [memefactory.server.contract.registry :as registry]
            [memefactory.server.contract.registry-entry :as registry-entry]
            [memefactory.server.macros :refer [promise->]]
            [memefactory.shared.smart-contracts-dev :refer [smart-contracts]]
            [memefactory.tests.smart-contracts.utils :refer [now create-before-fixture after-fixture]]
            [print.foo :include-macros true]))

(deftest deployment-tests
  (testing "testing if deployment was succesfull"
    (let [[addr0] (web3-eth/accounts @web3)]
      (async done
             (promise-> (ds-auth/authority :ds-guard)
                        #(is (= (contract-address :ds-guard) %))

                        #(dank-token/total-supply)
                        #(is (bn/= % (web3/to-wei 1000000000 :ether)))

                        #(dank-token/controller)
                        #(is (web3-utils/zero-address? %))

                        #(ds-auth/authority :meme-registry-db)
                        #(is (= (contract-address :ds-guard) %))

                        #(ds-auth/authority :param-change-registry-db)
                        #(is (= (contract-address :ds-guard) %))

                        #(ds-guard/can-call? {:src (contract-address :meme-registry-fwd)
                                              :dst (contract-address :meme-registry-db)
                                              :sig ds-guard/ANY})
                        #(is %)

                        #(ds-guard/can-call? {:src (contract-address :param-change-registry-fwd)
                                              :dst (contract-address :meme-registry-db)
                                              :sig ds-guard/ANY})
                        #(is %)

                        #(ds-guard/can-call? {:src (contract-address :meme-registry-fwd)
                                              :dst (contract-address :param-change-registry-db)
                                              :sig ds-guard/ANY})
                        #(is (false? %))

                        #(registry/is-factory? [:meme-registry :meme-registry-fwd] (contract-address :meme-factory))
                        #(is %)

                        #(registry/is-factory? [:param-change-registry :param-change-registry-fwd] (contract-address :param-change-factory))
                        #(is %)

                        #(dank-token/balance-of addr0)
                        #(is (bn/> % 0))

                        #(done)))




      )))
