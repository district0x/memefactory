(ns memefactory.ui.how.page
  (:require
    [district.ui.component.page :refer [page]]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [memefactory.ui.components.general :refer [nav-anchor]]))


(defn youtube-video [id]
  [:iframe.video {:width 560 :height 315 :src (str "https://www.youtube.com/embed/" id)}])


(defn a [href text]
  [:a {:href href :target :_blank} text])


(defmethod page :route.how-it-works/index []
  [app-layout
   {:meta {:title "MemeFactory - How It Works"
           :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.how-it-works-page
    [:div.how-it-works.panel
     [:div.icon]
     [:h2.title "How it works"]
     [:div.body
      [youtube-video "EoqF_3txiiI"]

      [:p "Meme Factory is a "
       [a "https://education.district0x.io/general-topics/understanding-ethereum/understanding-dapps/"
        "decentralized application (dApp)"]
       " running on the "
       [a "https://education.district0x.io/general-topics/understanding-ethereum/what-is-ethereum/"
        "Ethereum blockchain"]
       ". Meme Factory allows for the creation, collection, and curation of uniquely rare pieces of digital art. We call these “memes”, and they work like digital trading cards. Existing as "
       [a "https://education.district0x.io/general-topics/understanding-ethereum/erc-721-tokens/"
        "unique ERC721"]
       " tokens on the Ethereum blockchain, memes are submitted by individuals, voted into or out of the app by users via a "
       [a "https://education.district0x.io/general-topics/understanding-ethereum/token-curated-registry/"
        "token-curated registry (TCR)"]
       ", and then minted into existence through an open auction. Once the initial supply of any given meme is made, no more will be created. Meme owners and collectors are then free to trade and resell these rare memes - and thanks to the power of the blockchain, this can be done on or off the site through a system of "
       [a "https://education.district0x.io/general-topics/understanding-ethereum/what-are-smart-contracts/"
        "smart contracts"]
       "."

       [:h2.title.secondary "Setting up"]

       [:p "The first thing you will need is an Ethereum wallet. On a mobile device, you can download any mobile Ethereum browser from the playstore. We’d recommend "
        [a "https://wallet.coinbase.com/"
         "Coinbase Wallet"]
        ". The easiest way to do this on a desktop computer is by downloading a browser extension like "
        [a "https://metamask.io/"
         "MetaMask"]
        ". You can watch an installation tutorial below for each. "]

       [:div.badges
        [:a.metamask-wallet {:href "https://metamask.io/" :target :_blank} [:img {:src "/assets/images/how-it-works/metamask.png"}]]
        [:a.coinbase-wallet {:href "https://wallet.coinbase.com/" :target :_blank} [:img {:src "/assets/images/how-it-works/getCoinbaseWallet.svg"}]]]
       ;; [VIDEO - setting up MetaMask]
       [youtube-video "Sc8J98m6SZE"]

       ;; [VIDEO - setting up Coinbase Wallet]
       [youtube-video "D9tARxVzlL4"]

       [:h2.title.secondary "Browsing memes"]

       ;; [VIDEO - Browsing and searching]
       [youtube-video "NuUCxE7z_mA"]

       [:p "Browsing Memes is simple. Navigate to the list of memes for sale by clicking on the “District Registry” or “Browse” buttons on the left-hand menu. This will take you to the "
        [nav-anchor {:route :route.dank-registry/browse} "master registry list"]
        ", where all memes ever minted are shown. "]

       [:p "On this page, you can scroll through all memes and sort by time created or trade volume, search by name, or filter to find memes related to a certain topic, or even an individual meme. "]

       [:p "By clicking each meme, you can turn the card over and see more details about the creator, age, and other relevant information on its history, including links to explore and memes."]

       [:h2.title.secondary "Buying memes"]

       ;; [VIDEO - Buying memes]
       [youtube-video "KCSGVKQDep8"]

       [:p "Buying memes can be done by navigating to the "
        [nav-anchor {:route :route.marketplace/index} "Marketplace"]
        " page via the main menu. Here, similar to browsing memes, you can search and filter according to keywords and tags, and re-rank the page according to the time of creation, price of the memes, or the rarest memes. You can even choose to rank randomly, which will allow you to refresh the page for a completely new sorting of memes at your leisure."]

       [:p "To purchase any meme listed for sale, simply click it to flip it over, press the “Buy” button, and confirm the transaction. Memes you purchase will appear in the collected tab of the “"
        [nav-anchor {:route :route.memefolio/index} "My Memefolio"]
        "” page. "]

       [:h2.title.secondary "Getting DANK"]

       ;; [VIDEO - DANK Faucet]
       [youtube-video "uA8T_CzX1EY"]

       [:p "DANK is a brand new "
        [a "https://education.district0x.io/general-topics/understanding-ethereum/what-is-an-erc20-token/"
         "ERC20 token"]
        " that powers the Dank Registry, and gives Meme Factory users the power to vote on what memes get minted and which don’t, and facilitates more in-depth "
        [a "https://education.district0x.io/general-topics/what-is-governance/"
         "governance"]
        " over the parameters of the registry (when and how challenges are made and votes are won, how much of the reward goes to which party, and various other decisions). "]

       [:p "DANK can be acquired through several means, the most immediate of which is a one-time faucet which will dispense DANK to a connected Ethereum wallet. In order to claim yours, navigate to the DANK faucet via the left-hand navigation menu “"
        [nav-anchor {:route :route.get-dank/index} "Get DANK"]
        "” button. "]

       [:p "On this page, you can enter your phone number with country code and press the “submit” button. This will initiate an SMS verification (text message) to your number with a 4 digit code. Enter the code into the prompt on the DANK faucet page, and voila! After a few moments, your DANK will arrive in your connected wallet’s address."]

       [:p "Please note that by default, some wallets will not be tracking DANK balances, though they are stored correctly on the wallet’s address. You can always view your ETH and DANK balances for a connected address in the upper right hand corner of any "
        [nav-anchor {:route :route/home} "Meme Factory"]
        " page."]


       [:h2.title.secondary "Voting"]

       ;; [VIDEO - Voting]
       [youtube-video "tG_7I9l0gbQ"]

       [:p "With some DANK in hand, any Meme Factory user can begin voting on new meme submissions. Voting costs nothing, but winning votes will result in a small DANK reward. This means by always using your available DANK to vote, you will naturally accrue more DANK over time, and have more voting power to decide the outcome of each and every vote. "]

       [:p "In order to start voting, click the “"
        [nav-anchor {:route :route.dank-registry/vote} "Vote"]
        "” button in the main menu. On this page, the default view will display all open challenges against any memes trying to enter the registry. Open challenges have two phases - the vote phase and the reveal phase. You can see the details related to the current phase and challenge to the left-hand side of every meme. "]

       [:p "In the vote phase, any DANK holder can assign their available DANK balance to vote “DANK” to include a meme or “STANK” to reject it. Simply enter the amount you want to assign towards a particular vote, and click the corresponding button to the right of the meme."]

       [:img {:src "/assets/images/how-it-works/voting.png" :width "100%"}]



       [:p "During the reveal phase, any participant in the vote phase must return and reveal their votes to the public. Any votes that are not revealed are not counted, and are ineligible for a DANK reward. To reveal your votes on a meme you voted on, simply click the “REVEAL VOTE” button next to any meme where it’s available in the list of open challenges. If the “REVEAL VOTE” button doesn’t appear, no votes were made during the vote period from your connected Ethereum account."]

       [:img {:src "/assets/images/how-it-works/voting-reveal.png" :width "100%"}]



       [:p "To claim your voting reward, click the “"
        [nav-anchor {:route :route.dank-registry/vote
                     :query {:tab "Resolved Challenges"}} "Resolved Challenges"]
        "” tab on the “Vote” page. On this page, you can see all completed challenges that have occurred on Meme Factory. For each individual challenge, you can see the vote outcome and your participation, as well as any reward payouts. If your rewards are still unclaimed, there will be a button available for you to claim them. "]

       [:img {:src "/assets/images/how-it-works/voting-resolved.png" :width "100%"}]


       [:h2.title.secondary "Challenging"]

       ;; [Video - Challenging]
       [youtube-video "h3EYDMWsOfQ"]

       [:p "Challenging new memes entering the registry is a higher-risk, higher-reward means of earning DANK. Challenging a meme initiates a voting period, as described above. However, unlike simply voting for or against a meme where you can only gain DANK, starting a challenge requires the challenger to actually risk the same amount of DANK as the meme creator put forward to submit the meme. If the vote goes against the challenger, and the meme is accepted into the registry, the challenger will lose their DANK."]

       [:p "To get started challenging, click “"
        [nav-anchor {:route :route.dank-registry/challenge} "Challenge"]
        "” on the main menu. On this page, you’ll see every newly submitted meme to the registry still within its challenge period window. To the left, you can see details on the meme’s submission parameters,as well as the stats from the creator of that meme. On the right, there is a “CHALLENGE” button. Press this to bring up a text box. In this text box, type the exact reason for the challenge, and press the “CHALLENGE” button again in order to submit the transaction."]

       [:img {:src "/assets/images/how-it-works/challenge.png" :width "100%"}]


       [:p "Once the vote and reveal phases have concluded, rewards for challengers can be claimed in exactly the same way as rewards for vote participation, described above. This can also be done on the “"
        [nav-anchor {:route :route.memefolio/index :tab "curated"} "Curated"]
        "” tab of the “"
        [nav-anchor {:route :route.memefolio/index} "My Memefolio"]
        "” page. This page will allow you to sort through every challenge and/or vote you’ve ever participated in, using the checkboxes near the search bar. Each meme within this tab will include below it a current status - reflecting whether or not a vote has been revealed, or a reward for voting or challenging successfully has been claimed."]

       [:img {:src "/assets/images/how-it-works/memefolio.png" :width "100%"}]


       [:h2.title.secondary "Submitting Memes"]

       ;;[VIDEO - Submitting memes]
       [youtube-video "ztZg3n6SWhg"]

       [:p "The first step to submitting a popular meme is to create an image worth sharing. Meme Factory currently does not have the tools to support image creation, but we’ve written a guide for using the most popular external tools to create Meme Factory compatible images."]

       [:p "Once you have an image to upload, and with some DANK in your wallet, click on the “"
        [nav-anchor {:route :route.dank-registry/submit} "Submit"]
        "” button in the left-hand menu. From here, press the “UPLOAD A FILE” button to add your image, making sure it is of a compatible format and less than 1.5MB. "]

       [:p "On the right side of the page, fill out the forms for title, tags (these help people search and find this meme later, so be descriptive). You’ll also want to choose an issuance number - this is the number of individual cards you’ll be able to mint for sale of the submitted meme if accepted into the registry. "]

       [:img {:src "/assets/images/how-it-works/submit.png" :width "100%"}]

       [:p "With all the fields filled, press the “SUBMIT” button, and your meme will be uploaded and enter the challenging phase."]

       ]]]]])
