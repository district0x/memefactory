(ns memefactory.shared.smart-contracts-dev)

(def smart-contracts
  {:district-config
   {:name "DistrictConfig"
    :address "0x0000000000000000000000000000000000000000"}
   :ds-guard
   {:name "DSGuard"
    :address "0x0000000000000000000000000000000000000000"}
   :param-change-registry
   {:name "ParamChangeRegistry"
    :address "0x0000000000000000000000000000000000000000"}
   :param-change-registry-db
   {:name "EternalDb"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-registry-db
   {:name "EternalDb"
    :address "0x0000000000000000000000000000000000000000"}
   :param-change
   {:name "ParamChange"
    :address "0x0000000000000000000000000000000000000000"}
   :minime-token-factory
   {:name "MiniMeTokenFactory"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-auction-factory
   {:name "MemeAuctionFactory"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-auction
   {:name "MemeAuction"
    :address "0x0000000000000000000000000000000000000000"}
   :param-change-factory
   {:name "ParamChangeFactory"
    :address "0x0000000000000000000000000000000000000000"}
   :param-change-registry-fwd
   {:name "MutableForwarder"
    :address "0x0000000000000000000000000000000000000000"
    :forwards-to :param-change-registry}
   :meme-factory
   {:name "MemeFactory"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-token
   {:name "MemeToken"
    :address "0x0000000000000000000000000000000000000000"}
   :DANK
   {:name "DankToken"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-registry
   {:name "Registry"
    :address "0x0000000000000000000000000000000000000000"}
   :meme
   {:name "Meme"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-registry-fwd
   {:name "MutableForwarder"
    :address "0x0000000000000000000000000000000000000000"
    :forwards-to :meme-registry}
   :meme-auction-factory-fwd
   {:name "MutableForwarder"
    :address "0x0000000000000000000000000000000000000000"
    :forwards-to :meme-auction-factory}
   :district0x-emails
   {:name "District0xEmails"
    :address "0x0000000000000000000000000000000000000000"}
   :dank-faucet
   {:name "DankFaucet"
    :address "0x0000000000000000000000000000000000000000"}
   :ens
   {:name "ENS"
    :address "0x0000000000000000000000000000000000000000"}})
