(ns memefactory.ui.privacy-policy.page
  (:require
    [district.ui.component.page :refer [page]]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [memefactory.ui.components.general :refer [nav-anchor]]))


(defn a [href text]
  [:a {:href href :target :_blank} text])


(defmethod page :route.privacy-policy/index []
  [app-layout
   {:meta {:title "MemeFactory - Privacy Policy"
           :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.privacy-policy-page

    [:div.privacy-policy.panel
     [:div.icon]
     [:h2.title "Meme Factory Privacy Policy"]

     [:div.body


      [:p "Effective: May 1st, 2019"]

      [:p "At Meme Factory, we value the trust that you place in us when you give us personal information. We believe that privacy is more than an issue of compliance, and so we endeavor to manage personal information in accordance with our core value of respect for you and your privacy."]

      [:h2.title.secondary "Does the Privacy Policy Apply to You?"]

      [:p "If you create an account with Meme Factory, you will, upon registration, agree to our Privacy Policy."]

      [:h2.title.secondary "Policy Summary"]

      [:p "The information you share with Meme Factory allows us to provide you with the Services.  You may choose not to provide personal data to us, but if you choose not to provide personal data, you may not be able to take advantage of the Services we offer. "]

      [:p "The Policy includes:"
       [:ul
        [:li "What information we collect"]
        [:li "How we use your information"]
        [:li "How your personal information is protected"]
        [:li "How we share your personal information; and "]
        [:li "What choices you can make about how we collect, use, and share your personal information."]]]

      [:h2.title.secondary "What do we collect?"]

      [:p "We collect your registration information, which includes your name, your email address, and your wallet address.  In addition, we may collect data about your device and how you and your device interact with the Services, including usage patterns, location data, data about your device and your network, including your IP address, browser type, and operating system."]

      [:h2.title.secondary "Do we place cookies on your computer?"]

      [:p "When you access Meme Factory from a web browser, we place a cookie in a file accessed by your web browser.  Cookies are small files that a site or its service provider transfers to your hard drive through your Web browser (if you allow) that enables the sites or service providers systems to recognize your browser and capture and remember certain information. "]
      [:p "The cookies and related technology allow Meme Factory to help identify you, to save your preferences for future visits, to advertise to you on other sites, and to compile aggregate data about site traffic and site interaction so that we can offer better site experiences and tools in the future."]
      [:p "You may refuse to accept cookies by activating the appropriate setting on your browser, but if you do, your ability to use or access certain parts of the Service may be affected."]
      [:p "Our website may contain web beacons (aka single-pixel gifs) that we use to help deliver cookies on our website, and to count users who have visited certain web pages. We may also include web beacons in our promotional email messages or newsletters, to determine whether and when you open and act on them.  In addition, we may place web beacons or similar technologies from third-party analytics providers that help us compile aggregated statistics about the effectiveness of our promotional campaigns or other operations. These technologies enable the analytics providers to set or read their own cookies or other identifiers on your device, through which they can collect information about your online activities across applications, websites or other products."]

      [:h2.title.secondary "How do we use your information?"]

      [:p "We use the data to provide you with the Services, to communicate with you, provide security updates, send you marketing and account-related information, and respond to your inquiries.  We use data in the following ways:"]
      [:p [:b "Providing the Services."] " We use data to carry out your transactions on our website, and generally, to make the Services available to you. "]
      [:p [:b "Technical support."] " We use data to diagnose problems, and to provide customer care and other support services."]
      [:p [:b "Improving the Services."] " We use data to continually improve our website and the Services we provide on the website, including administration, security, and addition of features or capabilities."]
      [:p [:b "Business Operations."] " We use data to develop aggregate analyses and business intelligence that enable us to operate, protect, make informed decisions, and report on the performance of our business."]
      [:p [:b "Improving Advertising."] " We may use your data to improve our advertising, primarily in an effort to prevent targeting you with advertisement that are not relevant to you."]
      [:p [:b "Sending Periodic Emails."] " We may use your data to send you periodic marketing emails. "]
      [:p [:b "Generally."] " We use data to respond to enquiries and requests relating to the Services, to create and administer your account, and to provide us with information and access to resources that you have requested from us. We also use data for general business purposes, including, among other things, to improve customer service, to help us improve the content and functionality of the Services, to better understand our users, to protect against wrongdoing, to enforce our " [nav-anchor {:route :route.terms/index} "Terms of use"] ", and to generally manage our business."]
      [:p [:b "Communications."] " We use data we collect to communicate with you, to personalize our communications with you, and to allow you to communicate with others through our website. "]

      [:h2.title.secondary "How do we share your information?"]

      [:p "By registering with Meme Factory, you are giving us consent to share your personal data as outlined below.  "]
      [:p "In general, to the extent necessary to provide the Services or related activities, we also share your data with vendors working on our behalf; when required by law or to respond to legal process; to protect our customers; to protect lives; to maintain the security and integrity of our Game; and to protect our rights or our property."]
      [:p "We also share personal data with vendors or agents working on our behalf for the purposes described in this Privacy Policy. For example, companies we have hired to provide cloud hosting services, off-site backups, and customer support may need access to personal data to provide those functions. In such cases, these companies are required to abide by our data privacy and security requirements and are not allowed to use personal data they receive from us for any other purpose."]
      [:p "In addition, we may disclose your personal data as part of a corporate transaction such as a corporate sale, merger, reorganization, dissolution, or similar event. Finally, we will access, transfer, disclose, and/or preserve personal data, when we have a good faith belief that doing so is necessary to:"]
      [:p "(1) comply with applicable law or respond to valid legal process, judicial orders, or subpoenas;"]
      [:p "(2) respond to requests from public or governmental authorities, including for national security or law enforcement purposes;"]
      [:p "(3) protect the vital interests of our users, customers, or other third parties (including, for example, to prevent spam or attempts to defraud users of our products, or to help prevent the loss of life or serious injury of anyone);"]
      [:p "(4) operate and maintain the security or integrity of our Game, including to prevent or stop an attack on our computer systems or networks;"]
      [:p "(5) protect our rights, interests, or property, and the rights, interests, or property of third parties;"]
      [:p "(6) prevent or investigate possible wrongdoing in connection with the Services; or"]
      [:p "(7) enforce our " [nav-anchor {:route :route.terms/index} "Terms of use"] "."]
      [:p "We may use and share aggregated non-personal information with third parties for marketing, advertising, and analytics purposes. We do not sell or trade your personal information to third parties."]

      [:h2.title.secondary "Where do we keep personal data?"]

      [:p "We store personal data in the United States.  The storage location(s) are chosen to provide both security and efficiency in operation.  We transfer personal data from the European Economic Area and Switzerland to the United States. When we engage in such transfers, we use a variety of technical legal mechanisms, including contracts, to help ensure your rights and protections travel with your data. "]

      [:h2.title.secondary "How long do we keep your personal information?"]

      [:p "We may retain your personal information as long as necessary to provide you with the Services, as long as you have an account with us, or for as long as is necessary to fulfill the purposes outlined in this Policy. You can ask to close your account by contacting us as described above, and we will delete your personal information on request. We may, however, retain personal information for an additional period as is permitted or required under applicable laws, for legal, tax, or regulatory reasons, or for legitimate and lawful business purposes."]

      [:h2.title.secondary "Can you access, correct, or delete your personal information?"]

      [:p "Yes.  Your personal data is yours, and you can view, access, edit, delete, or request a copy of the personal data we collect.  You can also make choices about our collection and use of your data, including choosing whether to receive marketing communications from us. You can opt out from receiving marketing communications from us by using the opt-out link on the communication.  "]
      [:p "Data Erasure. You can delete your personal data by sending us an email to hello@district0x.io and requesting that we do so. Please be aware that we require certain information about you in order to provide you with the Services. Thus, if you send us a request to delete any personal data necessary to use the Services, you may be required to delete your entire profile and no longer be able to access your account or the Services on our website. We will use reasonable efforts to respond to your request within 14 days, but in all events within 30 days of our receipt of the request."]
      [:p "Data Correction. You can modify your personal data on your account page.  Alternatively, you may request that we modify your personal data by sending us an email to hello@district0x.io. Note that since some of the data we collect is specific to you – for example, your wallet address – you may not be able to modify this data without creating a new user profile."]
      [:p "Communications Preferences. You can choose whether you wish to receive marketing communications from us. If you receive marketing communications from us and would like to opt out, you can do so by following the directions in that communication.  Please note that opting out of marketing communications does not apply to communications that are necessarily a part of the Service. "]

      [:h2.title.secondary "Do we change our Privacy Policy?"]
      [:p "We will update this privacy statement when necessary to reflect customer feedback, changes to the Services, and changes in the law. When we post changes to this statement, we will revise the \"last updated\" date at the top of the statement. We encourage you to periodically review this privacy statement to learn how we protect your information."]

      [:h2.title.secondary "How can you contact us?"]

      [:p "If you have a technical or support question, please send us an email at hello@district0x.io."]
      [:p "If you have a privacy concern, complaint, or question, please contact us by sending us an email to hello@district0x.io. We will respond to questions or concerns within 30 days."]   ]]]])
