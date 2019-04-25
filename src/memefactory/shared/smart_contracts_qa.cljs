(ns memefactory.shared.smart-contracts-qa)

(def smart-contracts
  {:district-config
   {:name "DistrictConfig"
    :address "0xc0631861f334e80e960da6317f8b66a122b32e71"}
   :ds-guard
   {:name "DSGuard"
    :address "0xab4d684b2cc21ea99ee560a0f0d1490b09b09127"}
   :param-change-registry
   {:name "ParamChangeRegistry"
    :address "0xf67ef1ddec28f63832c0cc0293f46ebee377f82e"}
   :param-change-registry-db
   {:name "EternalDb"
    :address "0x8e27158eb1177b01bb4f0303c2b21ca785a55352"}
   :meme-registry-db
   {:name "EternalDb"
    :address "0x4450bcc33af0b93362fff45e957092b3d5371fe5"}
   :param-change
   {:name "ParamChange"
    :address "0xd5bb58cb0b26200238e2f0036968cb0e8875f278"}
   :minime-token-factory
   {:name "MiniMeTokenFactory"
    :address "0xa0a24fcac66bf51b2366686916e248c13a5641b1"}
   :meme-auction-factory
   {:name "MemeAuctionFactory"
    :address "0x51b786305b341f332538e74498e6d1e97b2b75d0"}
   :meme-auction
   {:name "MemeAuction"
    :address "0x57d3bd03852f7f7c40778b433f307e5d653379b9"}
   :param-change-factory
   {:name "ParamChangeFactory"
    :address "0x6446ae75abdef8ff35a20e49499c0f54e278f067"}
   :param-change-registry-fwd
   {:name "MutableForwarder"
    :address "0x5091c87601b085d5abb477a534bcac80fd11896e"
    :forwards-to :param-change-registry}
   :meme-factory
   {:name "MemeFactory"
    :address "0xdbddeb4d3d2d276286f82cfd5846835db91451d2"}
   :meme-token
   {:name "MemeToken"
    :address "0xed6c2bc38762a25ef9d2c3ea2aea417eba12f9b1"}
   :DANK
   {:name "DankToken"
    :address "0xeda9bf9199fab6790f43ee21cdce048781f58302"}
   :meme-registry
   {:name "Registry"
    :address "0xc709ef0d5a4161bb644d52107e9d1eb06fbf986e"}
   :meme
   {:name "Meme"
    :address "0x397b7e7152fb32d2c98826f182a15899aff5dc9c"}
   :meme-registry-fwd
   {:name "MutableForwarder"
    :address "0x4bc9e58e3dc6eb4dfdb783554bb2341ba160657b"
    :forwards-to :meme-registry}
   :meme-auction-factory-fwd
   {:name "MutableForwarder"
    :address "0xebe667d6b34e34920db1b5faf85f23ae6f003b9c"
    :forwards-to :meme-auction-factory}
   :district0x-emails
   {:name "District0xEmails"
    :address "0x3e6e8cdac0abab167644811b331594a500e8df7f"}
   :dank-faucet
   {:name "DankFaucet"
    :address "0xe26342c1eb2205a9f07711cd3733d934c6794206"}})