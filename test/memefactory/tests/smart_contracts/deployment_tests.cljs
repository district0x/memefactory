(ns memefactory.tests.smart-contracts.deployment-tests
  (:require [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.helpers :as web3-helpers]
            [cljs-web3-next.utils :as web3-utils]
            [cljs.core.async :refer [go <!]]
            [cljs.test :refer-macros [deftest is testing async]]
            [clojure.string :as string]
            [district.server.smart-contracts :refer [contract-address]]
            [district.server.web3 :refer [web3]]
            [memefactory.server.contract.dank-token :as dank-token]
            [memefactory.server.contract.ds-auth :as ds-auth]
            [memefactory.server.contract.ds-guard :as ds-guard]
            [memefactory.server.contract.registry :as registry]))

(deftest deployment-tests
  (async done
         (go
           (let [[addr0] (<! (web3-eth/accounts @web3))]

             (is (= (contract-address :ds-guard)
                    (string/lower-case (<! (ds-auth/authority :ds-guard)))))

             (is (= (<! (dank-token/total-supply))
                    (web3-utils/to-wei @web3 1000000000 :ether)))

             (is (web3-helpers/zero-address? (<! (dank-token/controller))))

             (is (= (contract-address :ds-guard)
                    (string/lower-case (<! (ds-auth/authority :meme-registry-db)))))

             (is (= (contract-address :ds-guard)
                    (string/lower-case (<! (ds-auth/authority :param-change-registry-db)))))

             (is (= true (<! (ds-guard/can-call? {:src (contract-address :meme-registry-fwd)
                                                  :dst (contract-address :meme-registry-db)
                                                  :sig ds-guard/ANY}))))


             (is (= false (<! (ds-guard/can-call? {:src (contract-address :meme-registry-fwd)
                                                   :dst (contract-address :param-change-registry-db)
                                                   :sig ds-guard/ANY}))))

             (is (= true (<! (registry/is-factory? [:meme-registry :meme-registry-fwd] (contract-address :meme-factory)))))

             (is (= true (<! (registry/is-factory? [:param-change-registry :param-change-registry-fwd] (contract-address :param-change-factory)))))

             (is "0" (<! (dank-token/balance-of addr0))))
           (done))))
