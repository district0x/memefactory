(ns memefactory.ui.about.page
  (:require [memefactory.ui.components.app-layout :refer [app-layout]]
            [district.ui.component.page :refer [page]]))

(defmethod page :route.about/index []
  [app-layout
   {:meta {:title "MemeFactory - About"
           :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.about-page

    [:div.about.panel
     [:div.icon]
     [:h2.title "About"]

     [:div.body
      [:p "Meme Factory was launched in early 2019 by the district0x team as the third district on the "
       [:a {:href "https://district0x.io/"} "district0x network"]
       ". Meme Factory is a platform for the creation, issuance, and exchange of provably rare digital collectibles on the Ethereum blockchain. Meme Factory users submit original memes to a community run list generally known as a "
       [:a {:href "https://education.district0x.io/district0x-specific-topics/meme-factory/what-is-a-tcr/"} "token curated registry"]
       ", and specifically known as the Dank Registry. Token holders of a newly minted token, the DANK token, then decide which memes are added to the registry through an economically incentivized voting game. Although DANK is an "
       [:a {:href "https://education.district0x.io/general-topics/understanding-ethereum/what-is-an-erc20-token/"} "ERC20 token"]
       " standard and therefore interchangeable, the actual memes themselves are ERC721 standard, more commonly referred to as "
       [:a {:href "https://education.district0x.io/general-topics/understanding-ethereum/erc-721-tokens/"} "Non-Fungible tokens (NFTs)"]
       ". This allows for unique identification, ownership, and provable rarity for every meme created on the Meme Factory platform. "]
      [:p "We welcome all contributions from the public. If you have any thoughts or suggestions on how we can improve, please stop by "
       [:a {:href "https://github.com/district0x/memefactory"} "Meme Factory’s GitHub"]
       ", the "
       [:a {:href "https://www.reddit.com/r/district0x/"} "district0x Reddit"]
       ", or the "
       [:a {:href "https://t.me/district0x"} "district0x Telegram"]
       "  to chat with the creators and community."]]]

    [:div.what-is.panel
     [:div.icon]
     [:h2.title "What is district0x?"]

     [:div.body
      [:p "District0x is a network of marketplaces and communities that exist as decentralized autonomous organizations on the district0x Network. All internet citizens will be able to deploy districts to the network free of charge, forever. The first district deployed; "
       [:a {:href "https://ethlance.com/"} "Ethlance"]
       ", is a decentralized job market which allows companies to hire and workers to find opportunities. "
       [:a {:href "https://namebazaar.io/"} "Name Bazaar"]
       ", the second district launched, is a peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service. Meme Factory is the third district launched; with more to follow."]]]


    [:div.more-info.panel
     [:div.icon]
     [:h2.title "More Information About district0x"]

     [:div.body
      [:p "The districts launched by district0x are built on a standard open source framework comprised of Ethereum smart contracts and front-end libraries, called d0xINFRA. This powers the various districts with core functionalities that are necessary to operate an online marketplace or community; including the ability for users to post listings, filter and search those listings, rank peers and gather reputation, send invoices and collect payments. The framework is designed to be open and extendable, allowing districts to be customized and granted additional functionality through the use of auxiliary modules - which can be thought of like “plug-ins” or “extensions.”"]
      [:p "District0x is powered by Ethereum, Aragon, and IPFS. district0x has its own token, "
       [:a {:href "https://coinmarketcap.com/currencies/district0x/"} "DNT"]
       ", which is used as a means of facilitating open participation and coordination on the network. DNT can be used to signal what districts should be built and deployed by the district0x team and can be staked to gain access to voting rights in any district on the district0x Network."]
      [:p "Perhaps the coolest thing about district0x is that you don't need to know how to code or have any technical skill to launch a district. If you dream it - the team can build it! district0x makes use of a "
       [:a {:href "https://github.com/district0x/district-proposals"} "district proposal process"]
       " to allow the community to determine what districts they would like to see built and deployed to the network next by the district0x team. Winning proposals are " [:a "voted on"] " by the community using DNT."]]]]])
