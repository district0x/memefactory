(ns memefactory.ui.home.tutorial)

(defn with-background [text]
  (str "<p style=\"background: rgba(100,100,100,0.8); padding:10px;\">"
       text
       "</p>"))

(def steps
  [
   #_1  {"custom div.marketplace" "At it's core, Meme Factory is a decentralized marketplace for collectible memes." :showSkip false}
   #_2  {"click .compact-tile:first>div" "Memes exist as provably rare tokens on the blockchain, and work a lot like digital trading cards. Click to flip over!" :showSkip false}
   #_3  {"next .compact-tile:first>div" "Memes are minted, auctioned, and sold on Meme Factory for Ethereum. Each meme has a fixed supply, and is provably rare. Once issued, no more will ever be made, and only the original supply can be traded and re-traded." :showSkip false}
   #_4  {"next .compact-tile:first>div>.flippable-tile-back>div>div>div>button" "If you find a meme you like listed for sale, simply click the buy button then submit and confirm the Ethereum transaction. Once purchased memes are yours to keep, sell, move, or trade away forever." :showSkip false}
   #_5  {"click div.memefolio" "At it's core, Meme Factory is a decentralized marketplace for collectible memes." :showSkip false}
   #_6  {"next .memefolio-page" (with-background "Your Memefolio contains your current collection of memes as well as a complete history of memes you've bought and sold.") :showSkip false}
   #_7  {"next section.tabs" "Meme Factory gives you the power not only to buy and sell on the marketplace, but control what enters the marketplace. You can <i>create</i> memes, and these memes are <i>curated</i> through a process of challenges and votes." :showSkip false}
   #_8  {"click div.faucet" "In order to start curating, you need a special token for the job, called DANK." :showSkip false}
   #_9  {"next .get-dank-box" "This page offers a one-time allotment of DANK tokens to get you started. It requires a phone number to prevent abuse. Don't worry - we'll show you how to earn more DANK next." :showSkip false}
   #_10 {"custom div.dank-registry-vote" "Once you've acquired some DANK, we can get started voting." :showSkip false}
   #_11 {"next .tabbed-panel" (with-background "This page shows all open challenges against new meme submissions. When a challenge is initiated, a period of voting begins. Any DANK you own can be used as voting power to decide whether a new meme goes to auction or gets rejected.") :showSkip false}
   #_12 {"next .challenge:first>div.info" "To the left, you can see all of the details of the creation of this meme, as well as the stated reason for the challenge. You can also see the success rates of the creator and challenger in their respective roles." :showSkip false}
   #_13 {"next .challenge:first>div.action" "Voting is simple. To the right, you can choose the amount of voting power you want to assign to \"DANK\" or \"STANK\". Your tokens will be returned to you in full, and if you're on the winning side, you'll even get a bit extra. So you should always be trying to vote with your DANK to earn more." :showSkip false}
   #_14 {"next .challenge:first>div.action" "When the voting period ends, you must return to this challenge and reveal your vote in order to make it public. This prevents any sort of influence from earlier votes. Just press the \"reveal\" button to the left of any meme you've voted on during the reveal period." :showSkip false}
   #_15 {"next .tabs-titles>.tab:last-child" " After the reveal period ends, the challenge moves to the resolved tab. Any rewards from winning votes or challenges can be claimed here." :showSkip false}
   #_16 {"custom div.dank-registry-challenge" "Starting a challenge is also easy. Keep in mind however, unlike voting, you can actually lose DANK while challenging. To get started, press the \"Challenge\" button." :showSkip false}
   #_17 {"next .challenge:first" (with-background "This page displays every new meme submitted for entry into the registry. All memes must survive a challenge period to make it into the registry uncontested. If challenged, memes will go to the voting phase.") :showSkip false}
   #_18 {"next .challenge:first>.action>.challenge-controls>button" "At any point during the challenge period, you may challenge a meme you don't like by matching and risking the DANK the meme creator used to submit the meme." :showSkip false}
   #_19 {"next div.dank-registry-vote" "New challenges move to the vote page. When voting concludes, rewards from complete challenges can be claimed from the \"Resolved Challenges\" tab on the vote page, or in your Memefolio." :showSkip false}
   #_20 {"click div.dank-registry-submit" "The final major feature of Meme Factory is, of course, creating memes. This is done with DANK on the \"Submit\" page." :showSkip false}
   #_21 {"next .image-panel" "To create a meme, find or create an appropriate image file. It's important to submit a meme with a vertical 2:3 aspect ratio, and less than 1.5MB. Most common image formats are accepted. If you need tips for creating memes, click <a href=\"/how-it-works\">here</>" :showSkip false}
   #_22 {"next .form-panel" (with-background "Submissions require names, and tags to index them for searching. Be creative, but also descriptive here. You must also choose a number of individual cards to mint and issue for sale. You can leave a comment to provide any other information you think challengers or voters might need.") :showSkip false}
   #_23 {"next div.dank-registry-challenge" "Memes that are submitted move to the Challenge phase. Any creations of yours can always be found in the \"Created\" tab of the \"My Memefolio\" page, where you can issue accepted memes for sale." :showSkip false}
   #_24 {"next div.my-settings" "The settings page allows you to link your email address in order to sign up for notifications. This will provide updates on any memes you've created, challenged, or voted for." :showSkip false}
   #_25 {"next div.how-it-works" "Any additional questions are likely answered in our \"How it works\" walkthrough. Welcome to Meme Factory!" :showSkip false}
   ])

(defn tutorial-button []
  [:div.tutorial-button {:on-click (fn []
                                     (set! (.-enjoy-hint-tutorial js/window)  (js/EnjoyHint.))
                                     (doto (.-enjoy-hint-tutorial js/window)
                                       (.set (clj->js steps))
                                       (.run)))}
   [:div "Guide Me"]
   [:img {:src "/assets/icons/guide-me-button-icon.svg"}]])
