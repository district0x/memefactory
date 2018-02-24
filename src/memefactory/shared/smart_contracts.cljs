(ns memefactory.shared.smart-contracts)

(def smart-contracts
  {:param-change-registry
   {:name "ParamChangeRegistry",
    :address "0xb348fd50caa5e7776fedcd891d907c0d6259e3fc"},
   :param-change-factory
   {:name "ParamChangeFactory",
    :address "0x4c3f1f5b1fae8cb68fa21d1b43260cc5ee7beebf"},
   :param-change
   {:name "ParamChange",
    :address "0xf262cac7018f4eebea44e6d1846bf20033a601bd"},
   :meme-registry-db
   {:name "EternalDb",
    :address "0xfc378fd4f28957cc2863d968c99eee01b2b68023"},
   :DANK
   {:name "DankToken",
    :address "0x7cb5590f0072690e400e214565998ba39751a4e0"},
   :meme-factory
   {:name "MemeFactory",
    :address "0xa7d129f835d7fe0caff27882de723d024a195ccd"},
   :meme-token
   {:name "MemeToken",
    :address "0x06b434d014ad82962e6c6c1bbd8e4a62bfb50e22"},
   :meme-registry
   {:name "Registry",
    :address "0x93cdff48f4d04e09f303cc855c82d9f9d6049035"},
   :meme
   {:name "Meme", :address "0xdd8244c9d720737e879d19094a5d966e8e85418e"},
   :meme-registry-fwd
   {:name "MutableForwarder",
    :address "0x262144830e9a8b048a9c43eb8d604f1f3ab2f584"},
   :param-change-registry-fwd
   {:name "MutableForwarder",
    :address "0xbae815bc7ab4b1f09707082702a72702b6c1a523"},
   :param-change-registry-db
   {:name "EternalDb",
    :address "0x358bca32f9fe51a2d30b04d7ab292981b69cc334"}
   :minime-token-factory
   {:name "MiniMeTokenFactory"
    :address "0x0000000000000000000000000000000000000000"}
   :ds-guard
   {:name "DSGuard"
    :address "0x0000000000000000000000000000000000000000"}})