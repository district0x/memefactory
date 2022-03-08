(ns memefactory.shared.smart-contracts-prod)

(def smart-contracts
  {:migrations {:name "Migrations" :address "0xB09b37814D9175B05727b257A2E86d7726E1557c"}
   :district-config {:name "DistrictConfig" :address "0x0B8c889DF1597BAc870eB9543AcB0CD38588F7f2"}
   :ds-guard {:name "DSGuard" :address "0xb0F0C547c6988075b939dc45d24A8924e6971555"}
   :ds-guard-root {:name "DSGuard" :address "0x5d0457f58ed4c115610a2253070a11fb82065403"}
   :meme-auth {:name "MemeAuth" :address "0x713cB16a8F0a2e898c605a309cEc4585Ca1712aE"}
   :param-change-registry {:name "ParamChangeRegistry" :address "0x9AB3cD06f8770dC1DedA74eCdF928c1CD5E26303"}
   :param-change-registry-db {:name "EternalDb" :address "0x01bF2878cA523b24acb4575B79AB81fCcaF928AA"}
   :meme-registry-db {:name "EternalDb" :address "0xdf8bCb12b4bC2A5beE67A6ec7e9Eda48ABD0B0eB"}
   :param-change {:name "ParamChange" :address "0x585A984669C2335495Bd69D6E9af058a19E475Cb"}
   :minime-token-factory {:name "MiniMeTokenFactory" :address "0x9D735BAF702f09ef7b89081E7a67D141866461d2"}
   :meme-auction-factory {:name "MemeAuctionFactory" :address "0x4284a10162BF37502c0F5Ec176D9675196275726"}
   :meme-auction {:name "MemeAuction" :address "0x209afEEa326618B3C4E2310251dF0e54E3972d59"}
   :param-change-factory {:name "ParamChangeFactory" :address "0x74D3af8d53C8dE0631d79F6ADB207d290415985F"}
   :param-change-registry-fwd {:name "MutableForwarder" :address "0xb25D7CE835B90C8Af70bA5a587975c5D9a95a907" :forwards-to :param-change-registry}
   :meme-factory {:name "MemeFactory" :address "0x0406942908050A5d51e7D15812083A4039F9F9ec"}
   :meme-token-root {:name "MemeToken" :address "0x4EDa5f84afAa6E3a89061AADa05138a6Be2e077d"}
   :meme-token {:name "MemeTokenChild" :address "0x5F50028E3D68Bb9F26b837229212dddBe5f36a14"}
   :DANK {:name "DankTokenChild" :address "0xfdF80c001f182E76894DEA7dD10e52D9Fb0F9715"}
   :DANK-root {:name "DankToken" :address "0x0cb8d0b37c7487b11d57f1f33defa2b1d3cfccfe"}
   :meme-registry {:name "Registry" :address "0x55b35217E038e7Dd33a5cd87d22EA6C123f25820"}
   :meme {:name "Meme" :address "0x9052e2a9965b53F09FA0B0E6A6BCe2ABA245682b"}
   :meme-registry-fwd {:name "MutableForwarder" :address "0x57F2A4115c2B6dD89eC8AFD258454B1352444FEE" :forwards-to :meme-registry}
   :meme-auction-factory-fwd {:name "MutableForwarder" :address "0xdBFacb099360DeF2234B0F42EfEFE7D0cc2275f6" :forwards-to :meme-auction-factory}
   :district0x-emails {:name "District0xEmails" :address "0x4c6Bcd0bD6764B915B8C4E18B6f5b8B6624b2581"}
   :dank-faucet {:name "DankFaucet" :address "0x3B793548292cD4C4bd955629FBf482ef25E8d998"}
   :ens {:name "ENS" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
   :DANK-child-controller {:name "DankChildController" :address "0x6B4B3D15F6A79bAE881A039c4484BC9eA05b73Bc"}
   :DANK-root-tunnel {:name "DankRootTunnel" :address "0x1baaF795cCA4c8aE6C4844d5BabF4994287a4E22"}
   :DANK-child-tunnel {:name "DankChildTunnel" :address "0x29Cdaf567a12e5e1C23f82AaD03E3D45d0ed86EC"}
   :meme-token-root-tunnel {:name "MemeTokenRootTunnel" :address "0x79fa3Ab4eABe55A7BF0B1E1D27B74a5365F30a4A"}
   :meme-token-child-tunnel {:name "MemeTokenChildTunnel" :address "0x4650103D7585008AA20D8b9a23dd789b2B633bEe"}})
