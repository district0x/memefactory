(ns memefactory.shared.smart-contracts-dev)

(def smart-contracts
  {:district-config
   {:name "DistrictConfig"
    :address "0x0000000000000000000000000000000000000000"}
   :DANK-root-tunnel
   {:name "DankRootTunnel"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-auth
   {:name "MemeAuth"
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
   :meme-token-root
   {:name "MemeToken"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-token-child-tunnel
   {:name "MemeTokenChildTunnel"
    :address "0x0000000000000000000000000000000000000000"}
   :minime-token-factory
   {:name "MiniMeTokenFactory"
    :address "0x0000000000000000000000000000000000000000"}
   :DANK-child-controller
   {:name "DankChildController"
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
   :meme-token-root-tunnel
   {:name "MemeTokenRootTunnel"
    :address "0x0000000000000000000000000000000000000000"}
   :param-change-registry-fwd
   {:name "MutableForwarder"
    :address "0x0000000000000000000000000000000000000000"
    :forwards-to :param-change-registry}
   :meme-factory
   {:name "MemeFactory"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-token
   {:name "MemeTokenChild"
    :address "0x0000000000000000000000000000000000000000"}
   :DANK
   {:name "DankTokenChild"
    :address "0x0000000000000000000000000000000000000000"}
   :DANK-child-tunnel
   {:name "DankChildTunnel"
    :address "0x0000000000000000000000000000000000000000"}
   :DANK-root
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
   :ens
   {:name "ENS"
    :address "0x0000000000000000000000000000000000000000"}
   :migrations
   {:name "Migrations"
    :address "0x0000000000000000000000000000000000000000"}
   :district0x-emails
   {:name "District0xEmails"
    :address "0x0000000000000000000000000000000000000000"}
   :ds-guard-root
   {:name "DSGuard"
    :address "0x0000000000000000000000000000000000000000"}
   :meme-auction-factory-fwd
   {:name "MutableForwarder"
    :address "0x0000000000000000000000000000000000000000"
    :forwards-to :meme-auction-factory}})
