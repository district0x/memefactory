(ns memefactory.shared.smart-contracts-prod)

(def smart-contracts
  {:migrations {:name "Migrations" :address "0xC9495C793aBb513CAcfEB961D5063ADdD363ad0B"} :district-config {:name "DistrictConfig" :address "0x82798CF7fCe9Ed8d29C2E35Ffc2Ccb8c418EEEae"} :ds-guard {:name "DSGuard" :address "0xEaf3391B5964eECe3712cA4ECdaa55Fb9AccAF83"} :ds-guard-root {:name "DSGuard" :address "0x5d0457f58ed4c115610a2253070a11fb82065403"} :meme-auth {:name "MemeAuth" :address "0x633be70EffC87a3c3E85385CCa074a365Fc250c2"} :param-change-registry {:name "ParamChangeRegistry" :address "0x7b303e6D1C55CF313cd8b6E5498aA8d2e6d94FAF"} :param-change-registry-db {:name "EternalDb" :address "0xBdA5d40Cf1B9bb7eBcB697f575af59db3f6795F1"} :meme-registry-db {:name "EternalDb" :address "0x8b6Da18BD8aD1D49788111a97D911F8B315e5A28"} :param-change {:name "ParamChange" :address "0x8aF932B6BDF72acbf50C766aeF578bF5AB6791af"} :minime-token-factory {:name "MiniMeTokenFactory" :address "0x712d0AE6E3Ed8e4AA75C82F077022bce2E02AE8D"} :meme-auction-factory {:name "MemeAuctionFactory" :address "0xd5556dDf99FE2C57e2474F758000dF299914bA86"} :meme-auction {:name "MemeAuction" :address "0x11F3E171D7C2207778acFc04CAD99E48f8e0Ba5c"} :param-change-factory {:name "ParamChangeFactory" :address "0x6B492561857065C1bfa90b1e825A227d2227F74f"} :param-change-registry-fwd {:name "MutableForwarder" :address "0xA8c97474dfb6d6AcAD543dA794eeE4Dbaec43512" :forwards-to :param-change-registry} :meme-factory {:name "MemeFactory" :address "0xe435223B32DE91226f14D87FF2214F46De185255"} :meme-token-root {:name "MemeToken" :address "0xCf946623619a362B66a386709E4898f168D46583"} :meme-token {:name "MemeTokenChild" :address "0x9051cC58C54D094a155091b4b84FaCa3ccfDbC1B"} :DANK {:name "DankTokenChild" :address "0xc055262B77167723270cC67CF4cD8af42999077B"} :DANK-root {:name "DankToken" :address "0x0cb8d0b37c7487b11d57f1f33defa2b1d3cfccfe"} :meme-registry {:name "Registry" :address "0x9C6c4fF54eC920bB07020061b4f7B8415C94cC9d"} :meme {:name "Meme" :address "0x6746A9Ad8f39682491E770b475fB3c66FF67568E"} :meme-registry-fwd {:name "MutableForwarder" :address "0x037e91F5c5CA26Ba67605ca8cDeA12ce9ef4e9F7" :forwards-to :meme-registry} :meme-auction-factory-fwd {:name "MutableForwarder" :address "0x475BbE7104EF34739422aCEaa88ADAedd0f38691" :forwards-to :meme-auction-factory} :district0x-emails {:name "District0xEmails" :address "0xaf64755d7bF19628fe58A5a4B51e3ecd0a1C9347"} :ens {:name "ENS" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"} :DANK-child-controller {:name "DankChildController" :address "0x5f2932ccf547c87109Fd39083f2CfC258057e31C"} :DANK-root-tunnel {:name "DankRootTunnel" :address "0xa697Dae09E90Be09d3c214596F9030aF423BeF19"} :DANK-child-tunnel {:name "DankChildTunnel" :address "0xAca88bbDedede5e8B385773760A5eF3847C3254A"} :meme-token-root-tunnel {:name "MemeTokenRootTunnel" :address "0xF1C3757cbddb574Cafb9375a7Eb50a0b24Ea625a"} :meme-token-child-tunnel {:name "MemeTokenChildTunnel" :address "0x2CCA62Cc08C9f2A1064A08a9ef2f84266135118b"} :dank-faucet {:name "DankFaucet" :address "0x0000000000000000000000000000000000000000"}})
