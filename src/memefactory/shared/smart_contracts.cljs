(ns memefactory.shared.smart-contracts) 

(def smart-contracts 
{:district-config
 {:name "DistrictConfig",
  :address "0x6d45076d4a87b3f85058eb85576753c0b07926a3"},
 :ds-guard
 {:name "DSGuard",
  :address "0xe47eeea38f293a77ece997b588485e476f07f029"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x4b6cdd7da1d72b9f6b27583a3e493893e8eb7a9d"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x47b966dda6858cf8a2495292ce8cbe859fa53b34"},
 :meme-registry-db
 {:name "EternalDb",
  :address "0x25e8adfddcff6b721b1542f789ff46385ae33a56"},
 :param-change
 {:name "ParamChange",
  :address "0x0eb025fda6348d02ed0141dbc8e4a2f3453d751b"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x6122e7eedf2fb0850eaa099656bdfa75d938200a"},
 :meme-auction-factory
 {:name "MemeAuctionFactory",
  :address "0x8cb025a31c00786447aac8fe3c06a0443de4a6ac"},
 :meme-auction
 {:name "MemeAuction",
  :address "0x95a987e436aac5dc67d2f4dfc1b5d63bb8b9162f"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x4c9a81fc475203bc903ff1c119f658b7558fd2c6"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x5daaeab0e720ff1697a963441ab1d54c0d034103"},
 :meme-factory
 {:name "MemeFactory",
  :address "0xe3c9660eccc58e2c69382cb6bf97ba9675663ece"},
 :meme-token
 {:name "MemeToken",
  :address "0x125c3c3c6227519751110beff4212f41a6adf95c"},
 :DANK
 {:name "DankToken",
  :address "0xfce295435454631e58311ce7fa8760615549d4cc"},
 :meme-registry
 {:name "Registry",
  :address "0x695acb01a93c27ada754684820adaa39ad5867de"},
 :meme
 {:name "Meme", :address "0xc0d445cbdae48ed1165614a99a1d084f403513fb"},
 :meme-registry-fwd
 {:name "MutableForwarder",
  :address "0x9164f2ecd753014a9363fde7be9b4b18769c1694"},
 :meme-auction-factory-fwd
 {:name "MutableForwarder",
  :address "0xe77b82c6407cd6fce49540cacd3efc1bb890ce60"}})