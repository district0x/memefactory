(ns memefactory.ui.terms.page
  (:require
    [district.ui.component.page :refer [page]]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [memefactory.ui.components.general :refer [nav-anchor]]))


(defn a [href text]
  [:a {:href href :target :_blank :rel "noopener noreferrer"} text])


(defmethod page :route.terms/index []
  [app-layout
   {:meta {:title "MemeFactory - Terms of Use"
           :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.terms-page

    [:div.terms.panel
     [:div.icon]
     [:h2.title "Meme Factory Terms of Use"]

     [:div.body


      [:p "Effective: May 1st, 2019"]


      [:p "Welcome to MEME FACTORY!  These Terms of Use (these “Terms”) govern your access to and use of the Meme Factory website; including without limitation the creation, uploading, purchase, sale, exchange, or modification of digital assets through our website (the “Services”).  Our " [nav-anchor {:route :route.privacy-policy/index} "Privacy Policy"] " explains how we collect and use your information while our Acceptable Use Policy outlines your responsibilities when using our Services. By using our Services, you’re agreeing to be bound by these Terms, our " [nav-anchor {:route :route.privacy-policy/index} "Privacy Policy"] " and Acceptable Use Policy. If you are using our Services for an organization, you’re agreeing to these Terms on behalf of that organization."]
      [:p "MEME FACTORY is a SELF-GOVERNING platform for the creation, issuance, and exchange of provably rare digital collectibles on the Ethereum blockchain. By utilizing a “token curated registry”, called the Dank Registry, Meme Factory users create and submit original memes to a community run list, where token holders of a newly minted token, the DANK token, decide what makes the cut. By leveraging the Ethereum blockchain and token-curated registries, Meme Factory allows for the emergence of a true decentralized digital collectible platform, with the users of the platform controlling everything from the brands & categories recognized, the memes certified for issuance, the scarcity of the meme (think common and rare memes), and even the fees required to trade them."]
      [:p "MEME FACTORY IS A PLATFORM. WE ARE NOT A BROKER, FINANCIAL INSTITUTION, OR CREDITOR. THE SERVICES ARE AN ADMINISTRATIVE PLATFORM ONLY TO FACILITATE CERTAIN TRANSACTIONS, INCLUDING CREATING, UPLOADING, BUYING, SELLING, EXCHANGING, OR MODIFYING DIGITAL COLLECTIBLES. WE ARE NOT A PARTY TO ANY AGREEMENT BETWEEN THE BUYER AND SELLER OF DIGITAL ASSETS OR BETWEEN ANY USERS."]
      [:p "When you use our Services, you provide us with data that may include your files, content, messages, and so on (“Your Stuff”). Your Stuff is yours. These Terms do not give us any rights to Your Stuff except for the limited rights that enable us to offer the Services."]
      [:p "We sometimes need to change or add terms to this agreement, so agree to regularly check this web page. Any changes or additions will be posted on this website, and immediately become part of your agreement with us if you use those services."]
      [:p "THESE TERMS OF USE ARE IMPORTANT AND AFFECT YOUR LEGAL RIGHTS, SO PLEASE READ THEM CAREFULLY"]

      [:h2.title.secondary "License Grant"]
      [:p "Subject to your adherence to the terms and conditions in this Agreement, you are hereby granted a personal, limited, nonexclusive, nontransferable, nonsublicensable, license to access and use the Services.  "]

      [:h2.title.secondary "License Limitations "]
      [:p "You agree that you do not have a license to, and will not: (i) sell or resell the Service; (ii) distribute or display any content you do not own; (iii) modify the Service; (iv) use any data mining, robots, or similar data and content gathering or extraction methods; and (v) use the Service other than for its intended purposes.  "]

      [:h2.title.secondary "Your Responsibilities"]
      [:p "You are responsible for your conduct.  By creating an account, you are agreeing to comply with our "  "Acceptable Use Policy and our " [nav-anchor {:route :route.privacy-policy/index} "Privacy Policy"] ". Parts of the Services, or any content found thereon, may be protected by others’ intellectual property rights. You must not create, copy, upload, download, or share content unless you have the right to do so, and you agree to absolve us of any responsibility for your violation of these terms.  BY USING THIS WEBSITE AND THESE SERVICES, YOU AGREE THAT WE ARE NOT RESPONSIBLE FOR THE CONTENT POSTED AND SHARED VIA THE SERVICES, AND YOU AGREE TO INDEMNIFY US FOR ANY LIABILITY OR COSTS INCURRED RELATED TO YOUR USE."]
      [:p "You agree to Safeguard your password to the Services, keep your account information current, and not share your account credentials with others or give others access to your account."]
      [:p "You may use our Services only as permitted by applicable law, including export control laws and regulations. Finally, to use our Services, you must be at least 13 years old. If you live in France, Germany, or the Netherlands, you must be at least 16. Please check your local law for the age of digital consent. If you don’t meet these age requirements, you are not allowed to use the Services."]

      [:h2.title.secondary "Account Registration"]
      [:p "If you wish to utilize the Services, including curating content, you will need to create an account on the Meme Factory website (the “Account”).  By creating your Account, you agree to (a) provide accurate, current, and complete Account information about yourself; (b) maintain and promptly update your Account information to keep that information current and accurate; (c) maintain the security of your password; (d) accept all risks associated with using a password, blockchain technology, and token-curated registries; and (e) immediately notify us if you discover or otherwise suspect any security breaches related to the Service or your Account.  You understand and agree that we may pause or cancel any transaction initiated on our website, especially if any illegal or allegedly illegal activity is brought to our attention."]
      [:p "By creating an Account, you are giving us your consent to receive electronic communications from us.  You agree that any notices, agreements, disclosures or other electronic communications received from us satisfies, and will satisfy, any legal requirements for such communication.  You may opt out of receiving these promotional emails at any time by following the unsubscribe instructions contained in the email."]
      [:p "Notwithstanding anything to the contrary in this Agreement, any third-party software and content is governed by the terms of use of such software and content."]

      [:h2.title.secondary "Fees and Payment"]

      [:p "If you elect to create, upload, buy, or sell, collectibles through the Services, any financial transactions that you engage in will be conducted solely through the Ethereum network. We will have no insight into or control over these payments or transactions, nor do we have the ability to reverse any transactions. With that in mind, you agree that we will have no liability to you or to any third party for any claims or damages that may arise as a result of any transactions that you engage on our website, or any other transactions that you conduct via the Ethereum network."]
      [:p "Ethereum requires the payment of a transaction fee (a “Gas Fee”) for every transaction that occurs on the Ethereum network. The Gas Fee funds the network of computers that run the decentralized Ethereum network. This means that you will need to pay a Gas Fee for each transaction that occurs via the App."]
      [:p "You agree that you are be solely responsible to pay any and all taxes related to your use of the Services, including but not limited to sales, use, value-added and other taxes, duties, and assessments (except taxes on our net income) now or hereafter claimed or imposed by any governmental authority (collectively, “Taxes”). Except for income taxes levied on us, you: (i) will pay or reimburse us for all national, federal, state, local or other taxes and assessments of any jurisdiction, including value added taxes and taxes as required by international tax treaties, customs or other import or export taxes, and amounts levied in lieu thereof based on charges set, services performed or payments made hereunder, as are now or hereafter may be imposed under the authority of any national, state, local or any other taxing jurisdiction; and (ii) shall not be entitled to deduct the amount of any such taxes, duties or assessments from payments made to us pursuant to these Terms. You confirm that you are not a resident in Canada nor are you registered for Goods and services tax / Harmonized sales tax (GST / HST) or Provincial sales taxes (PST) in Canada, and will inform us if your status changes in the future."]

      [:h2.title.secondary "Software"]
      [:p "Some of our Services may allow you to download client software (“Software”) that may update automatically. So long as you comply with these Terms, we give you a limited, nonexclusive, nontransferable, revocable license to use the Software, solely to access the Services. To the extent any component of the Software may be offered under an open source license, we’ll make that license available to you and the provisions of that license may expressly override some of these Terms. Unless the following restrictions are prohibited by law, you agree not to reverse engineer or decompile the Software and the Services, attempt to do so, or assist anyone in doing so."]

      [:h2.title.secondary "Intellectual Property"]
      [:p "The Services are protected by copyright, trademark, and other U.S. and foreign laws.  Nothing in this Agreement grants you any right, title, or interest in the Services, others’ content in the Services, Meme Factory trademarks, logos, and other brand feature.  We encourage feedback, but if you give us feedback, you hereby give us permission to use that feedback without limitation or obligation."]
      [:p "If we receive notice, or otherwise suspect, that content you uploaded, bought, or sold, violates a third-party copyright or trademark, you agree that we may suspend or cancel the transaction, remove the content at issue, and suspend or cancel your Account, all without any liability to us.  You understand and agree that we will comply with any proper law-enforcement inquiry, subpoena, or court order.  "]

      [:h2.title.secondary "Third Party Websites and Services"]
      [:p "Any third-party websites, services, and applications accessed from the Meme Factory website are governed by their own terms of use, and if you access such a third-party website, service, or application, you are subject to the terms of use that govern each.  By accessing such third-party website, service, or application, from the Meme Factory website, you understand and agree that such third-party websites are not under our control, and that we are not responsible for any behavior or actions that occur on such third-party website, service, or application.  "]

      [:h2.title.secondary "Indemnification"]
      [:p "To the fullest extent permitted by law, you agree to indemnify, defend, and hold harmless, District0x,, including our employees, officers, directors, contractors, consultants, suppliers, vendors, service providers, affiliates, agents, representatives, and any predecessor or successor thereto, from and against all actual or alleged third party claims, damages, awards, judgments, losses, liabilities, obligations, penalties, interest, fees, expenses (including, without limitation, attorneys’ fees and expenses) and costs (including, without limitation, court costs, costs of settlement and costs of pursuing indemnification and insurance), of every kind and nature whatsoever, whether known or unknown, foreseen or unforeseen, matured or unmatured, or suspected or unsuspected, in law or equity, whether in tort, contract or otherwise (collectively, “Claims”), including, but not limited to, damages to property or personal injury, that are caused by, arise out of or are related to (a) your use or misuse of the Service, content, collectibles or other crypto assets, (b) any feedback you provide, (c) your violation of these Terms, and (d) your violation of the rights of a third party. You agree to promptly notify us of any third-party claims and cooperate with us in defending such claims. You further agree that we shall have control of the defense or settlement of any third-party claims. THIS INDEMNITY IS IN ADDITION TO, AND NOT IN LIEU OF, ANY OTHER INDEMNITIES SET FORTH IN A WRITTEN AGREEMENT BETWEEN YOU AND MEME FACTORY."]

      [:h2.title.secondary "Disclaimers"]
      [:p "EXCEPT AS EXPRESSLY PROVIDED TO THE CONTRARY IN A WRITING BY DISTRICT0X, TO THE EXTENT ALLOWED BY LAW, THE SERVICE, CONTENT CONTAINED THEREIN, COLLECTIBLES, AND OTHER CRYPTO ASSETS LISTED THEREIN, ARE PROVIDED ON AN “AS IS” AND “AS AVAILABLE” BASIS WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED. DISTRICT0X (AND ITS SUPPLIERS) MAKE NO WARRANTY THAT THE SERVICE: (A) WILL MEET YOUR REQUIREMENTS; (B) WILL BE AVAILABLE ON AN UNINTERRUPTED, TIMELY, SECURE, OR ERROR-FREE BASIS; OR (C) WILL BE ACCURATE, RELIABLE, COMPLETE, LEGAL, OR SAFE. DISTRICT0X DISCLAIMS ALL OTHER WARRANTIES OR CONDITIONS, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, IMPLIED WARRANTIES OR CONDITIONS OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT AS TO THE SERVICE, CONTENT CONTAINED THEREIN. DISTRICT0X DOES NOT REPRESENT OR WARRANT THAT CONTENT ON THE SERVICE IS ACCURATE, COMPLETE, RELIABLE, CURRENT OR ERROR-FREE. WE WILL NOT BE LIABLE FOR ANY LOSS OF ANY KIND FROM ANY ACTION TAKEN OR TAKEN IN RELIANCE ON MATERIAL OR NONMATERIAL INFORMATION, CONTAINED ON THE SERVICE. WHILE DISTRICT0X ATTEMPTS TO MAKE YOUR ACCESS TO AND USE OF THE SERVICE AND CONTENT SAFE, WECANNOT AND DO NOT REPRESENT OR WARRANT THAT THE SERVICE, CONTENT, COLLECTIBLES, AND ANY OTHER CRYPTO ASSETS, LISTED ON OUR SERVICE OR OUR SERVERS ARE FREE OF VIRUSES OR OTHER HARMFUL COMPONENTS. WE CANNOT GUARANTEE THE SECURITY OR PRIVACY OF ANY DATA THAT YOU DISCLOSE ONLINE. YOU ACCEPT THE INHERENT SECURITY RISKS OF PROVIDING INFORMATION AND DEALING ONLINE OVER THE INTERNET, INCLUDING ANY RISKS INHERENT IN CREATING, USING, OR TRADING DIGITAL ASSETS OR COLLECTIBLES, AND YOU AGREE YOU WILL NOT HOLD US RESPONSIBLE FOR ANY BREACH OF SECURITY UNLESS IT IS DUE TO OUR GROSS NEGLIGENCE."]
      [:p "WE WILL NOT BE RESPONSIBLE OR LIABLE TO YOU FOR ANY LOSS AND TAKE NO RESPONSIBILITY FOR, ANY USE OF DIGITAL COLLECTIBLES OR OTHER CRYPTO ASSETS, INCLUDING BUT NOT LIMITED TO ANY LOSSES, DAMAGES OR CLAIMS ARISING FROM: (A) USER ERROR SUCH AS FORGOTTEN PASSWORDS, INCORRECTLY CONSTRUCTED TRANSACTIONS, OR MISTYPED ADDRESSES; (B) SERVER FAILURE OR DATA LOSS; (C) CORRUPTED WALLET FILES; (D) UNAUTHORIZED ACCESS TO APPLICATIONS; (E) ANY UNAUTHORIZED THIRD-PARTY ACTIVITIES, INCLUDING WITHOUT LIMITATION THE USE OF VIRUSES, PHISHING, BRUTEFORCING OR OTHER MEANS OF ATTACK AGAINST THE SERVICE, THE DIGITAL COLLECTIBLES, OR CRYPTO ASSETS."]
      [:p "DIGITAL COLLECTIBLES ARE INTANGIBLE DIGITAL ASSETS. THEY EXIST ONLY BY VIRTUE OF THE OWNERSHIP RECORD MAINTAINED IN THE ETHEREUM NETWORK. ANY TRANSFER OF TITLE THAT MIGHT OCCUR IN ANY UNIQUE DIGITAL ASSET OCCURS ON THE DECENTRALIZED LEDGER WITHIN THE ETHEREUM PLATFORM. WE DO NOT GUARANTEE THAT WE CAN EFFECT THE TRANSFER OF TITLE OR RIGHT IN ANY CRYPTO ASSETS."]
      [:p "DISTRICT0X EXPRESSLY DISCLAIMS ANY LIABILITY RELATED TO OR CAUSED BY INTERRUPTIONS OF THE SERVICE.  "]
      [:p "DISTRICT0X is not responsible for sustained casualties due to vulnerability or any kind of failure, abnormal behavior of software (e.g., wallet, smart contract), blockchains or any other features of the Crypto Assets. DISTRICT0X is not responsible for casualties due to late report by developers or representatives (or no report at all) of any issues with the blockchain supporting Crypto Assets including forks, technical node issues or any other issues having fund losses as a result."]
      [:p "Nothing in these Terms shall exclude or limit liability of either party for fraud, death or bodily injury caused by negligence, violation of laws, or any other activity that cannot be limited or excluded by legitimate means."]

      [:h2.title.secondary "Limitation of Liability"]
      [:p "TO THE FULLEST EXTENT PERMITTED BY LAW, IN NO EVENT WILL DISTRICT0X BE LIABLE TO YOU OR TO ANY THIRD PARTY FOR ANY LOST PROFITS OR ANY INDIRECT, CONSEQUENTIAL, EXEMPLARY, INCIDENTAL, SPECIAL OR PUNITIVE DAMAGES ARISING FROM THESE TERMS, THE SERVICE AND YOUR USE THEREOF, THE COLLECTIBLES ON THE MEME FACTORY SITE, USE OF ANY DIGITAL ASSETS SUCH AS CRYPTOCURRENCY, TANGIBLE AND INTANGIBLE PRODUCTS OR THIRD PARTY SITES AND TANGIBLE OR INTANGIBLE PRODUCTS, OR FOR ANY DAMAGES RELATED TO ANY LOSS OF REVENUE, PROFITS, BUSINESS OR ANTICIPATED SAVINGS, USE, GOODWILL, OR DATA, WHETHER CAUSED BY TORT (INCLUDING NEGLIGENCE), BREACH OF CONTRACT, OR OTHERWISE, EVEN IF FORESEEABLE AND EVEN IF WE HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. ACCESS TO, AND USE OF, THE SERVICES ARE AT YOUR OWN RISK, AND YOU WILL BE SOLELY RESPONSIBLE FOR ANY DAMAGE TO YOUR HARDWARE OR SOFTARE, INCLUDING ANY COMPUTER SYSTEM OR MOBILE DEVICE, OR LOSS OF DATA RESULTING THEREFROM."]
      [:p "NOTWITHSTANDING ANYTHING TO THE CONTRARY CONTAINED HEREIN, IN NO EVENT SHALL THE MAXIMUM AGGREGATE LIABILITY OF DISTRICT0X ARISING OUT OF OR IN ANY WAY RELATED TO THESE TERMS, THE ACCESS TO AND USE OF THE SERVICE, CONTENT, CRYPTO ASSETS, OR ANY PRODUCTS OR SERVICES PURCHASED ON THE SERVICE EXCEED THE GREATER OF (A) $100 OR (B) THE AMOUNT RECEIVED BY US FOR USE OF THE SERVICE WITHIN THE 12 MONTHS PRIOR TO THE RELEVANT CLAIM."]
      [:p "THE FOREGOING LIMITATIONS OF LIABILITY SHALL NOT APPLY TO LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY OUR NEGLIGENCE; OR FOR (B) ANY INJURY CAUSED BY OUR FRAUD OR FRAUDULENT MISREPRESENTATION"]
      [:p "In accessing the website or using the Services, you have reviewed and understand the relevant risk, and you accept and acknowledge the following:"]
      [:ul
       [:li "The prices of blockchain assets, including digital collectibles and crypto currency, are extremely volatile. We cannot guarantee that any party using the Services or creating or trading in digital collectibles, will not lose money. "]
       [:li "You are solely responsible for determining what, if any, taxes apply to your Crypto Assets transactions.  We are not responsible for and will not withhold taxes that apply to your use of the Services or your creation and or dealings with digital collectibles. "]
       [:li "Digital collectibles and crypto currency exist solely by virtue of the ownership record maintained on its supporting blockchain.  Thus, we do not store, send, or receive digital collectibles and digital currency."]
       [:li "You understand the risks associated with using an Internet based currency, including but not limited to, hardware- and software-based risks, and risks inherent in access the Services, and you agree that we will not be responsible for any communication failures, disruptions, errors, distortions or delays you may experience when using the Services."]
       [:li "The regulatory regime governing blockchain technologies, cryptocurrencies, and tokens is uncertain, and new regulations or policies may materially adversely affect the development of the Auction and/or Service and the utility of Crypto Assets."]]

      [:h2.title.secondary "Modifications"]
      [:p "We reserve the right in our sole discretion to modify, suspend or discontinue, temporarily or permanently, the Services (or any features or parts thereof) or suspend or discontinue the Auction at any time and without liability therefore and for any reason whatsoever."]
      [:p "We may revise these Terms from time to time to better reflect (a) changes to the law, (b) new regulatory requirements, or (c) improvements or enhancements made to our Services."]

      [:h2.title.secondary "Dispute Resolution"]
      [:p "If you have any concerns, we want to address them without needing a formal legal case. Before filing a claim against us, you agree to try to resolve the dispute informally by contacting us directly. We will try to resolve the dispute by contacting you via email. If a dispute is not resolved within 15 days of submission, you or we may bring a formal proceeding."]
      [:p "Judicial forum for disputes. Unless you reside in a country that provides consumers with the right to bring dispute in local courts, You agree that any judicial proceeding to resolve claims relating to these Terms or the Services will be brought in the federal or state courts of San Francisco County, California, subject to the mandatory arbitration provisions below. You consent to venue and personal jurisdiction in such courts."]
      [:p "IF YOU ARE A U.S. RESIDENT, YOU ALSO AGREE TO THE FOLLOWING MANDATORY ARBITRATION PROVISIONS:"]
      [:p "You and DISTRICT0X agree to arbitrate any claim, rather than resolving the claim through the courts. You agree to resolve any claims relating to these Terms or the Services through final and binding arbitration by a single arbitrator, except as set forth under Exceptions to Agreement to Arbitrate below. This includes disputes arising out of or relating to interpretation or application of this arbitration provision, including its enforceability, revocability, or validity."]
      [:p "Opt-out of Agreement to Arbitrate. You may decline this agreement to arbitrate by emailing us at hello@district0x.io and requesting an opt-out within 30 days of first registering your account. If, however, you agreed to a previous version of these Terms that allowed you to opt out of arbitration, your previous choice to opt out, or not, remains binding."]
      [:p "Arbitration Procedures. Arbitration will be conducted according to the rules of, and will be administered by, The American Arbitration Association (AAA), under its Commercial Arbitration Rules and the Supplementary Procedures for Consumer Related Disputes. The arbitration will be held in San Francisco (CA), or any in other location we agree to."]
      [:p "Arbitration Fees and Incentives. The AAA rules will govern payment of all arbitration fees. We will not seek its attorneys' fees and costs in arbitration unless the arbitrator determines that your claim is frivolous."]
      [:p "Exceptions to Agreement to Arbitrate. If applicable, either you or we may assert claims in small claims court in San Francisco (CA) or any United States county where you live or work. Either party may bring a lawsuit solely for injunctive relief to stop unauthorized use or abuse of the Services, or intellectual property infringement (for example, trademark, trade secret, copyright, or patent rights) without first engaging in arbitration or the informal dispute-resolution process described above. If the agreement to arbitrate is found not to apply to you or your claim, you agree to the exclusive jurisdiction of the state and federal courts in San Francisco County, California to resolve your claim."]

      [:p [:b "NO CLASS ACTIONS"]". You may only resolve disputes with us on an individual basis, and you agree not to bring a claim as a plaintiff or a class member in a class, consolidated, or representative action. Class arbitrations, class actions, private attorney general actions, and consolidation with other arbitrations are not permitted under this Agreement. If this specific paragraph is held unenforceable, then the entirety of this “Mandatory Arbitration Provisions” section will be deemed void."]
      [:p "Notwithstanding anything contained in these Terms, we reserve the right, without notice and in our sole discretion, to terminate your right to access or use the Service at any time and for any or no reason, and you acknowledge and agree that we shall have no liability or obligation to you in such event and that you will not be entitled to a refund of any amounts that you have already paid to us, to the fullest extent permitted by applicable law."]

      [:h2.title.secondary "Severability"]

      [:p "If any term, clause, or provision of this Agreement is held invalid or unenforceable, such invalidity or unenforceability will not affect any other term, clause, or provision of this Agreement."]

      [:h2#acceptable-use-policy.title.secondary "Acceptable Use Policy"]
      [:p "Because we use token-curated registries, you agree as a condition to using the Services, that (i) you will provide us with accurate and complete account-registration information; (ii) you will not create more than one account, and (iii) you understand that we will block or disable multiple accounts of the same user.  Once your account is blocked or disabled, you agree not to create another account without our prior-written permission."]
      [:p "In addition, as a condition to using the Services, you agree that you will not:"]

      [:ul
       [:li "Use or attempt to use another user’s Account without authorization from both such user and from us; "]
       [:li "Use the Services or in any manner, or take any action, that could interfere with, disrupt, negatively affect or inhibit other users from fully enjoying the Service, or that could damage, disable, overburden or impair the functioning of the Service in any manner;"]
       [:li "Reverse engineer any aspect of the Service, or do anything that might discover source code or bypass or circumvent measures employed to prevent or limit access to any Service, area or code of the Service; "]
       [:li "Attempt to circumvent any content-filtering techniques we employ, or attempt to access any feature or area of the Service that you are not authorized to access; "]
       [:li "Use any robot, spider, crawler, scraper, script, browser extension, offline reader or other automated means or interface not authorized by us to access the Service, extract data or otherwise interfere with or modify the rendering of Service pages or functionality; "]
       [:li "Use data collected from our Service to contact individuals, companies, or other persons or entities; "]
       [:li "Use data collected from our Service for any direct marketing activity (including without limitation, email marketing, SMS marketing, telemarketing, and direct marketing); "]
       [:li "Bypass or ignore instructions that control all automated access to the Service; "]
       [:li "Use the Service for any illegal or unauthorized purpose, or engage in, encourage or promote any activity that violates these Terms; or "]
       [:li "Use the Service or the Ethereum Platform to carry out any illegal activities, including but not limited to money laundering, terrorist financing or deliberately engaging in activities designed to adversely affect the performance of the Ethereum Platform, or the Service."]]


                                                                                       ]]]])
