(ns memefactory.shared.smart-contracts) 

(def smart-contracts 
{:district-config
 {:name "DistrictConfig",
  :address "0xe9de074ee8f67ebe88f366b83215aac3553247b7"},
 :ds-guard
 {:name "DSGuard",
  :address "0x21a7e08930759c4308d18a103fe1827badf9ba3a"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x0672e088923e3933a4d7d6620a200fbfa1e3ae94"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x4f2e9bc69dc2b23e4c917fbfa85fb25686bd0828"},
 :meme-registry-db
 {:name "EternalDb",
  :address "0x95033b76fb42387182af61e419f5fcde2d76c14d"},
 :param-change
 {:name "ParamChange",
  :address "0x74166a9474a80a073a86ad2d8984f07c1028bacf"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0xff2624f2b12969e4741a79cbf887633be78922fa"},
 :meme-auction-factory
 {:name "MemeAuctionFactory",
  :address "0x95c24ef95e5a94e27a5ad133882b2d224c79b3ea"},
 :meme-auction
 {:name "MemeAuction",
  :address "0xf1592389fd7756a6c4fa7dd526d34fa582924d1d"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x9e70de82a91d8361332dddc6a6d139077c9147cb"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x7465a3b934fbf3f16bf58359d08532062eef7dfd"},
 :meme-factory
 {:name "MemeFactory",
  :address "0x02acd0ea9888e8276a7d5df715fd77ca99b5265a"},
 :meme-token
 {:name "MemeToken",
  :address "0x70df47cc5983874aaa346f63159f3e1ef196db64"},
 :DANK
 {:name "DankToken",
  :address "0x7f38bf9d4d3ea04b9a24777b878d9cbfb9ddd0e4"},
 :meme-registry
 {:name "Registry",
  :address "0x22843e74c59580b3eaf6c233fa67d8b7c561a835"},
 :meme
 {:name "Meme", :address "0xe20dc9adf01a73fbef5d5d66c9ccfb389ae8f03f"},
 :meme-registry-fwd
 {:name "MutableForwarder",
  :address "0xed9af5e4c2ad19669a417df2b1934bbdf5bb4f71"},
 :meme-auction-factory-fwd
 {:name "MutableForwarder",
  :address "0xed65ec68116d2a18ea1a178cd44243533150a46b"}})