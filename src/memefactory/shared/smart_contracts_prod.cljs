(ns memefactory.shared.smart-contracts-prod)

(def smart-contracts
  {:district-config
   {:name "DistrictConfig",
    :address "0xc3f953d1d9c0117f0988a16f2eda8641467e0b6d"},
   :dank-faucet
   {:name "DankFaucet",
    :address "0x7abdcd059a60ad6d240a62be3fe0293fb2b65c19"},
   :ds-guard
   {:name "DSGuard",
    :address "0x5d0457f58ed4c115610a2253070a11fb82065403"},
   :param-change-registry
   {:name "ParamChangeRegistry",
    :address "0xb08306e7187e73ae5f22a114208c590754f2efaf"},
   :param-change-registry-db
   {:name "EternalDb",
    :address "0x8dd52b82ca30d70a212c20fd77456a9ae32b9e16"},
   :meme-registry-db
   {:name "EternalDb",
    :address "0xb59ff0854090af74f4d56e9bc2d7e80e29a17b8d"},
   :param-change
   {:name "ParamChange",
    :address "0xd6734c56ba99db79d5388a7f4daaf1690ecde806"},
   :minime-token-factory
   {:name "MiniMeTokenFactory",
    :address "0x7cc607ba48f19ea817525c546cbec2bbae8d1499"},
   :meme-auction-factory
   {:name "MemeAuctionFactory",
    :address "0x2b75458dfa6a7dc7061dd2a468e7ed22970d997e"},
   :meme-auction
   {:name "MemeAuction",
    :address "0x92268d9c15657f14c9b0d551b97e260467dbe585"},
   :param-change-factory
   {:name "ParamChangeFactory",
    :address "0x44d28a7e9649ef2774540846255d9238e37c0e02"},
   :param-change-registry-fwd
   {:name "MutableForwarder",
    :address "0x942b6b83b654761b13fba7b230b9283ddec08f2c",
    :forwards-to :param-change-registry},
   :meme-factory
   {:name "MemeFactory",
    :address "0xe9a45e9c0ac1c9393091c77a2ed0eb51f4f04f20"},
   :meme-token
   {:name "MemeToken",
    :address "0xd23043ce917ac39309f49dba82f264994d3ade76"},
   :DANK
   {:name "DankToken",
    :address "0x0cb8d0b37c7487b11d57f1f33defa2b1d3cfccfe"},
   :meme-registry
   {:name "Registry",
    :address "0x8f24fb009e0ed2a342d56b5246ef69f72d297744"},
   :meme
   {:name "Meme", :address "0x1f342d5118176b6b01c7bf7a9f733d7b748b2431"},
   :meme-registry-fwd
   {:name "MutableForwarder",
    :address "0xe278b85a36f6b370347d69fb4744947e2965c058",
    :forwards-to :meme-registry},
   :migrations
   {:name "Migrations",
    :address "0xfcdf47d4ca1effaa6759829f6985f6a5e4e54ae1"},
   :district0x-emails
   {:name "District0xEmails",
    :address "0x5065ef0724b76421aeaafa87ba752d6c5d5499b5"},
   :meme-auction-factory-fwd
   {:name "MutableForwarder",
    :address "0xb47c930fa2cce89d0a92925733ae65f72ae8914e",
    :forwards-to :meme-auction-factory},
   :ens
   {:name "ENS",
    :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}})
