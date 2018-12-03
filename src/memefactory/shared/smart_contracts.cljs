(ns memefactory.shared.smart-contracts) 

(def smart-contracts 
{:district-config
 {:name "DistrictConfig",
  :address "0x0573d116d06795922cdb6bd669e3afaa71f17587"},
 :dank-faucet
 {:name "DankFaucet",
  :address "0xbbd5679d91f207d0312e931057eff45d6446f96a"},
 :ds-guard
 {:name "DSGuard",
  :address "0x5815c85b03f688e48256ff750417f5abde0471be"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0xd3e2e8d93a2d6a48360cf1fff8f81095fdfe3e67"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x1b2cdb62ebfe7af4ccfecb56c4f441690ae3a18e"},
 :meme-registry-db
 {:name "EternalDb",
  :address "0xaddb7eebaad4fb119d9a77b75712b4a50f08b064"},
 :param-change
 {:name "ParamChange",
  :address "0x60d94ee554aba07704e019f047ff4de531af71be"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0xbc64ab85254e1244ac9d43ab548914b5fe8f8332"},
 :meme-auction-factory
 {:name "MemeAuctionFactory",
  :address "0x902e7db9d579ee987d72f18ec9fbf2acf4b34764"},
 :meme-auction
 {:name "MemeAuction",
  :address "0xb8e6ae78434d96ccb6963d5abb53c6d5c48f0999"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0xca30d31bb87048f06a3257c0a7816d77bcb75653"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x1b3766ef455485e123e1c469e64ccfe886a69eb9"},
 :meme-factory
 {:name "MemeFactory",
  :address "0x0e587e488f32ad0f3fefc07383abbdc741406349"},
 :meme-token
 {:name "MemeToken",
  :address "0xa5d3dec9576dfedbd06bcdbb32e2b1632ab92563"},
 :DANK
 {:name "DankToken",
  :address "0x886da90c34b0f01cd641a0ff3dad28cc3d7641a4"},
 :meme-registry
 {:name "Registry",
  :address "0x7f18f22d8b045212c0f0b7aa3000c22d73b0685a"},
 :meme
 {:name "Meme", :address "0xf1a064d950857f42eefd7b99f262d56c0305fe59"},
 :meme-registry-fwd
 {:name "MutableForwarder",
  :address "0x984cdfc9cd1eefdc4c1114b1e07693ec702c0193"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x5eb647415704f97921b41afb9192a1cb1fc551aa"},
 :meme-auction-factory-fwd
 {:name "MutableForwarder",
  :address "0x8a1020e70b9f29036018acfd479c085f8d53acdd"}})