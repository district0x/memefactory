(ns memefactory.shared.smart-contracts-qa)
(def smart-contracts
  {:migrations {:name "Migrations" :address "0x75d327Ee9AFF234396b966e04031d04cD4d7EB70"}
   :district-config {:name "DistrictConfig" :address "0xdac7884e994eabd1ae7cd667d663cc416d9ec5c2"}
   :ds-guard {:name "DSGuard" :address "0xc95b3e8d8d47269f5b284e922368b4c7a11e570a"}
   :param-change-registry {:name "ParamChangeRegistry" :address "0x13819300c07dcbae1eb90d7f52e15633b5a04ca4"}
   :param-change-registry-db {:name "EternalDb" :address "0x9053604c0c63675a6e006066b720f00ef0d5594a"}
   :meme-registry-db {:name "EternalDb" :address "0x94f3e4b09b9bc52fc105caad74c4394db4978b40"}
   :param-change {:name "ParamChange" :address "0x167b87d8e918baedbcee33d958027ebf9dc269b8"}
   :minime-token-factory {:name "MiniMeTokenFactory" :address "0x855f49ae96f7c557c376130a868a2c6da5bfad9d"}
   :meme-auction-factory {:name "MemeAuctionFactory" :address "0x52db21409f86ec1a1f9592d6a076ce212d909342"}
   :meme-auction {:name "MemeAuction" :address "0x1d58335f34104963dd6ce15fcc60ed041fdc8f96"}
   :param-change-factory {:name "ParamChangeFactory" :address "0xa235405e6479ef052626f12801c502441c8e608a"}
   :param-change-registry-fwd {:name "MutableForwarder" :address "0xc2de159ca5175da01a972c3a7e41ec0579a62948" :forwards-to :param-change-registry}
   :meme-factory {:name "MemeFactory" :address "0x5e98d96812ad6a1b3f9740c19d92db1e5d3174b3"}
   :meme-token {:name "MemeToken" :address "0xceb6cc4c64256e4c9c45fc89eeab1851bcc7bc3b"}
   :DANK {:name "DankToken" :address "0xc527437dce612326d4915865f730befe1c89f995"}
   :meme-registry {:name "Registry" :address "0xeab2fbcb0a4e83029d03582ed1bbf45435eed0cf"}
   :meme {:name "Meme" :address "0xde54bbe8705a0a8999fdbd4f531012f6f5c604ef"}
   :meme-registry-fwd {:name "MutableForwarder" :address "0x798693bdb05e9359b8d30d8d052044974aedaa53" :forwards-to :meme-registry}
   :meme-auction-factory-fwd {:name "MutableForwarder" :address "0xa4681530e9826a36b70b18b3b35893788ebe1f4f" :forwards-to :meme-auction-factory}
   :district0x-emails {:name "District0xEmails" :address "0xaff9758d2693ce469da913dc7d64ed256f318eed"}
   :dank-faucet {:name "DankFaucet" :address "0xb2295316e4012fa49bc4787157301abca9188ae4"}
   :ens {:name "ENS" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}})
