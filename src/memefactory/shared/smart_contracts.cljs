(ns memefactory.shared.smart-contracts)

(def smart-contracts
  {:district-config
   {:name "DistrictConfig",
    :address "0xddf5a2745ee964d1ed9330cacaf9ea2ea7e33933"},
   :ds-guard
   {:name "DSGuard",
    :address "0x74fc6891ef3c2b4d22f594fdd9dea5c9f1a123a9"},
   :param-change-registry
   {:name "ParamChangeRegistry",
    :address "0x80bc878ad3403c61029518eec252c5d8c928a068"},
   :param-change-registry-db
   {:name "EternalDb",
    :address "0x78aa4a53f69ecf28a096d56cbb0b386ee7c823ac"},
   :meme-registry-db
   {:name "EternalDb",
    :address "0x6060208885fc5da057250cc29db49578833c1505"},
   :param-change
   {:name "ParamChange",
    :address "0x2db5614f54013a52748b02fd70a337298d83b35e"},
   :minime-token-factory
   {:name "MiniMeTokenFactory",
    :address "0x7182bca490d13f736184d04d411f8e3ab5e68b3b"},
   :meme-auction-factory
   {:name "MemeAuctionFactory",
    :address "0x9b23f3a2ddd4b3fc7d38ab92e0ff9a8538fcebe6"},
   :meme-auction
   {:name "MemeAuction",
    :address "0xfc337cef3e5c38493bf2aad12eb03d57a213128d"},
   :param-change-factory
   {:name "ParamChangeFactory",
    :address "0xd656b5934cfde2647e221f56e2aba54b52cedf7b"},
   :param-change-registry-fwd
   {:name "MutableForwarder",
    :address "0xa544418293a3a5492079223845cf6b2803332cbf"},
   :meme-factory
   {:name "MemeFactory",
    :address "0x4b1885c9b5a515102636b8cdd7b96a18a04e0d15"},
   :meme-token
   {:name "MemeToken",
    :address "0x052e92854252beae3c26cccfa997a4d08798cbdb"},
   :DANK
   {:name "DankToken",
    :address "0x72e322505444dcac8fddb5655e33e6eac7a361be"},
   :meme-registry
   {:name "Registry",
    :address "0x5fba9838de80590778c1a54ad9bfe7d907b2c55a"},
   :meme
   {:name "Meme", :address "0x377dbbfd2dac0f02438393e0706dbb94f4c2f309"},
   :meme-registry-fwd
   {:name "MutableForwarder",
    :address "0xafdb130331899f526f84fdf34a97703e8819c2c4"},
   :meme-auction-factory-fwd
   {:name "MutableForwarder",
    :address "0x773e8bfc01322645cc9c26db0297f7c8aa4cb4fa"}
   :dank-faucet
   {:name "DankFaucet",
    :address "0x91facb67d424113a6584da2d42460014f9db9159"}
   :district0x-emails
   {:name "District0xEmails"
    :address "0xe6305ce0348f9646ad4601725b383a3fb4f3f2f0"}})
