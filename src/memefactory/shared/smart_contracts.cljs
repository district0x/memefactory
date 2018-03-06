(ns memefactory.shared.smart-contracts) 

(def smart-contracts 
{:ds-guard
 {:name "DSGuard",
  :address "0xfdd0f1203ae24c938cc8db1dd7375632134fc466"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x6d7b1f1c1471e4c554ce5ba8ebbeea6c7d75cc92"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0xf576bb0d3913d991a3bc61bbf1d03ab29a88aba4"},
 :meme-registry-db
 {:name "EternalDb",
  :address "0xa93df12f2ef14cf8523a1a42bfd311daef381c59"},
 :param-change
 {:name "ParamChange",
  :address "0xa461dc1c9bb283a8f0061064ae4f028e9df8a638"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x0f4bd08f3927511367b3c1d009489b80356508b2"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0xaa681f377c6a06045ffd1b092ce915f8ca846451"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x349d1be4c5d0a69c6e4617f8cb8ed0ec67608feb"},
 :meme-factory
 {:name "MemeFactory",
  :address "0xbce9c9dd9ffb3aa818eb26f1e0efeae85fd9d859"},
 :meme-token
 {:name "MemeToken",
  :address "0xab823dfca62faecaa0c648cf15d3eaafbd389604"},
 :DANK
 {:name "DankToken",
  :address "0xf26032b09570b58f43e919a67c3fdcd5a03c0c1e"},
 :meme-registry
 {:name "Registry",
  :address "0x647e7657fe384490b4ad95c894d6c7e244c38c69"},
 :meme
 {:name "Meme", :address "0x6158d39c013123a656f495add0dfaecdc3130b1a"},
 :meme-registry-fwd
 {:name "MutableForwarder",
  :address "0xd11b10a03fcd1b11663329f20209cd18213d58dd"}})