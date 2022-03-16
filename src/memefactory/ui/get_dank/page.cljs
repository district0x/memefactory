(ns memefactory.ui.get-dank.page
  (:require
   [clojure.string :as str]
   [district.ui.component.form.input :refer [index-by-type text-input with-label]]
   [district.ui.component.page :refer [page]]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.shared.utils :refer [tweet-url-regex]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.buttons :refer [chain-check-button]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.components.share-buttons :as share-buttons]
   [memefactory.ui.get-dank.events :as dank-events]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log]
   [memefactory.ui.components.general :refer [nav-anchor]]))

(defn a [href text]
  [:a {:href href :target :_blank :rel "noopener noreferrer"} text])

(defn tweet-url-format? [tweet-url]
  (re-matches tweet-url-regex tweet-url))

(defmethod page :route.get-dank/index []
  (let [form-data (r/atom {})
        errors (reaction {:local (let [{:keys [tweet-url]} @form-data]
      (cond-> {}
              (str/blank? tweet-url)
              (assoc-in [:tweet-url :error] "Tweet URL is mandatory")

              (and (not (str/blank? tweet-url)) (not (tweet-url-format? tweet-url)))
              (assoc-in [:tweet-url :error] "Tweet URL not valid")
              ))})
      critical-errors (reaction (index-by-type @errors :error))]
    (fn []
      (let [show-spinner? @(subscribe [:memefactory.ui.subs/dank-faucet-spinner])
            succeeded? @(subscribe [:memefactory.ui.subs/dank-faucet-succeeded])
            active-account @(subscribe [::accounts-subs/active-account])
            address (if active-account active-account "[address]")
            text (str "I'm verifying my address " address " and claiming my free $DANK tokens on @memefactory0x so I can vote and challenge memes.\n\nGo claim your free $DANK on #Polygon today: https://memefactory.io/get-dank.")]
        [app-layout
         {:meta {:title "MemeFactory - Receive initial DANK tokens"
                 :description "Make a tweet with your Ethereum address and we'll send you a one-time allotment of DANK tokens. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.get-dank-page
          [:div.get-dank-box
           [:div.icon]
           [:h2.title "Receive initial DANK tokens"]
           [:h3.buttons (chain-check-button :button.button {:on-click (fn [] (dispatch [::dank-events/import-dank]))} "Add DANK to your wallet" ) ]
           [:h3.title "To get DANK, make a tweet including your Polygon address using the Tweet button below."]
           [:div.share-button
            [share-buttons/twitter-share-button {:url text} [share-buttons/twitter-icon {:size 40 :round true}] "Make a tweet"]]
           [:h3.title "Then, copy-paste the tweet URL into the input box below and you'll receive a one-time allotment of DANK tokens"]
           [:div.body
            [:div.form
              [with-label
               "Tweet URL"
               [text-input {:form-data form-data
                            :errors errors
                            :key :tweet-url
                            :dom-id :tweet-url
                            :id :tweet-url
                            :placeholder "https://twitter.com/username/status/12345678901234567890"}]
               {:form-data form-data
                :for :tweet-url
                :id :tweet-url}]]
           (when show-spinner?
            [spinner/spin])]

            [:button.footer
                           {:on-click (fn []
                                        (let [tweet-url (:tweet-url @form-data)]
                                          (dispatch [::dank-events/get-allocated-dank-twitter tweet-url])))
                            :disabled (or succeeded? show-spinner? (not (empty? @critical-errors)))}
                           "Submit"]
            ]]]))))

