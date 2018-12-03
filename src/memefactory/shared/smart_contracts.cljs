(ns memefactory.shared.smart-contracts) 

(def smart-contracts 
{:district-config
 {:name "DistrictConfig",
  :address "0x1b2cdb62ebfe7af4ccfecb56c4f441690ae3a18e"},
 :dank-faucet
 {:name "DankFaucet",
  :address "0xb8e6ae78434d96ccb6963d5abb53c6d5c48f0999"},
 :ds-guard
 {:name "DSGuard",
  :address "0x886da90c34b0f01cd641a0ff3dad28cc3d7641a4"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0xf2f23b823fd05aed5d8625636ec23aaf74636026"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x984cdfc9cd1eefdc4c1114b1e07693ec702c0193"},
 :meme-registry-db
 {:name "EternalDb",
  :address "0xd3e2e8d93a2d6a48360cf1fff8f81095fdfe3e67"},
 :param-change
 {:name "ParamChange",
  :address "0xbf621fa20e08d25f4cbf9e0153a030025272b413"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x05b1c1abaed1ffb152cd96a5cd531a562dc7ff94"},
 :meme-auction-factory
 {:name "MemeAuctionFactory",
  :address "0x5eb647415704f97921b41afb9192a1cb1fc551aa"},
 :meme-auction
 {:name "MemeAuction",
  :address "0x8cc8321fc9d39f6e62482cd9b52f9b640114e28a"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0xf5d1b25c911d6c284abdf907801d84a16e6bf1e6"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0x4e98bbe5bcc22f8925fba4e80405b40ddc913f5d"},
 :meme-factory
 {:name "MemeFactory",
  :address "0x560c4e0c84a1231fab1969cd0757c8db99caa9c6"},
 :meme-token
 {:name "MemeToken",
  :address "0x0e587e488f32ad0f3fefc07383abbdc741406349"},
 :DANK
 {:name "DankToken",
  :address "0xaddb7eebaad4fb119d9a77b75712b4a50f08b064"},
 :meme-registry
 {:name "Registry",
  :address "0x1b3766ef455485e123e1c469e64ccfe886a69eb9"},
 :meme
 {:name "Meme", :address "0xca30d31bb87048f06a3257c0a7816d77bcb75653"},
 :meme-registry-fwd
 {:name "MutableForwarder",
  :address "0xe3f31a44228ce59c8d1a7e5ee38be7b6e60288e4"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x0a4f348684c0e6b0b415364427c5115ebde112cb"},
 :meme-auction-factory-fwd
 {:name "MutableForwarder",
  :address "0x902e7db9d579ee987d72f18ec9fbf2acf4b34764"}})