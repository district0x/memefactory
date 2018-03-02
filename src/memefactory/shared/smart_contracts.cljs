(ns memefactory.shared.smart-contracts) 

(def smart-contracts 
{:ds-guard
 {:name "DSGuard",
  :address "0x8e47701d60d8eb5a33cf257231ff34754398f753"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0xd7ccb9b162452fa02f41041666ec726414dcd83e"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0xfe030b09dc0aa0948c03b64ed490431e49ac5286"},
 :meme-registry-db
 {:name "EternalDb",
  :address "0x979e1f32d9f0bd2006d467f10a5752b06f60a121"},
 :param-change
 {:name "ParamChange",
  :address "0xba357c615efedd2b6e1cdce6db69b24236b7f597"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0xcf7245f20a33eee9a1ca1063ce117f62b6a0426a"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x7d128ebc8430f78a0375730c87d800b33cf86908"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x562b51434544376976bf09f1a4b89e25390c88a5"},
 :meme-factory
 {:name "MemeFactory",
  :address "0xdbc2f19414b3cf01561509ee8471f542655cc6ce"},
 :meme-token
 {:name "MemeToken",
  :address "0x7f1e26ff5861d7906e36550b4145e5788d1e6ccc"},
 :DANK
 {:name "DankToken",
  :address "0x7fb7393436ae79e379840226e1cae8fb77741b8a"},
 :meme-registry
 {:name "Registry",
  :address "0x2ef2e13e10f06d92813a40d0f689f904c9ef0389"},
 :meme
 {:name "Meme", :address "0xfa1581ea327bb88eed72be786f734e652c734c55"},
 :meme-registry-fwd
 {:name "MutableForwarder",
  :address "0xddeac56be70026a30745978f8446c8b0b04e7dc2"}})