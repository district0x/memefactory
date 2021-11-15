(ns memefactory.shared.smart-contracts-qa)

(def smart-contracts
  {:migrations {:name "Migrations" :address "0xA13f966373b1E8B14541496cA88C6149606dCc8c"}
    :district-config {:name "DistrictConfig" :address "0x866C8207E0E6F66B5424008ec6Aa6a53bc7e0d50"}
    :ds-guard {:name "DSGuard" :address "0xFd73A17d55756974C683f6a30293991c7d1Ba459"}
    :ds-guard-root {:name "DSGuard" :address "0xe6D9C7fe47eeE2C032D38e01C951F9cF6F5fEc03"}
    :meme-auth {:name "MemeAuth" :address "0x7e5304219aEb762F2fC4d06AeC913b666A0AF021"}
    :param-change-registry {:name "ParamChangeRegistry" :address "0x7D61605c218FcE7eCC9cdcb57d6bdCa80d622228"}
    :param-change-registry-db {:name "EternalDb" :address "0xd8358126de5361a34c5c6f678E922261A1c414Bf"}
    :meme-registry-db {:name "EternalDb" :address "0x1f78E5734206E7aabC424e5b6353ac60e51dD1F6"}
    :param-change {:name "ParamChange" :address "0x80c29BE2529Ed0e8A8755bFe34Aa80F65A2843c0"}
    :minime-token-factory {:name "MiniMeTokenFactory" :address "0xe6D9C7fe47eeE2C032D38e01C951F9cF6F5fEc03"}
    :meme-auction-factory {:name "MemeAuctionFactory" :address "0x242656FA5e542B316772D55EFB2e2DF8D837A27B"}
    :meme-auction {:name "MemeAuction" :address "0x891Bf5a942995e2d72578869E56d05D282C6544f"}
    :param-change-factory {:name "ParamChangeFactory" :address "0x662e04d39d464270790b0777269C4Bb1e084f05c"}
    :param-change-registry-fwd {:name "MutableForwarder" :address "0x95E7fC4047f912CA43830a510CF8145741056583" :forwards-to :param-change-registry}
    :meme-factory {:name "MemeFactory" :address "0xB560FA23633bD1bE1420966Ff843a19812560d82"}
    :meme-token-root {:name "MemeToken" :address "0x60c7Feef93459e8e8183F9C77a5df67ab3fDe9F4"}
    :meme-token {:name "MemeTokenChild" :address "0x662d13e1D4D01C5CeCEeEEB05a772cEBbcbD4C5f"}
    :DANK {:name "DankTokenChild" :address "0xaA3BA7666fB74650f18828c367cED8f82C9907B7"}
    :DANK-root {:name "DankToken" :address "0xea0381E5469aEace9A20eB3dBd4512a5cb332d9D"}
    :meme-registry {:name "Registry" :address "0x60c7Feef93459e8e8183F9C77a5df67ab3fDe9F4"}
    :meme {:name "Meme" :address "0x9170c254989Cf1B077f7a464fdcDEc8E30905495"}
    :meme-registry-fwd {:name "MutableForwarder" :address "0x32A5b4995a0677312B0Cd745b493a7a6b1Ee2966" :forwards-to :meme-registry}
    :meme-auction-factory-fwd {:name "MutableForwarder" :address "0x5454FBAc6EDEfC8e597cC0f9E272c9076DBd674D" :forwards-to :meme-auction-factory}
    :district0x-emails {:name "District0xEmails" :address "0xDAeF3341c6F82Cee565777321b7e79bDe4ED78D4"}
    :ens {:name "ENS" :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"}
    :DANK-child-controller {:name "DankChildController" :address "0xfCEE0eA06a4af53D08d2C4F276F67b2c8E87CfDb"}
    :DANK-root-tunnel {:name "DankRootTunnel" :address "0x866C8207E0E6F66B5424008ec6Aa6a53bc7e0d50"}
    :DANK-child-tunnel {:name "DankChildTunnel" :address "0x7BB697Eff146b5F3e79FA5612bD00F0c03d8Bc74"}
    :meme-token-root-tunnel {:name "MemeTokenRootTunnel" :address "0x662e04d39d464270790b0777269C4Bb1e084f05c"}
    :meme-token-child-tunnel {:name "MemeTokenChildTunnel" :address "0xb06059c2a9d0DfAAba8c91D106Bc99ee0b50BF14"}})
