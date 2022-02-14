(ns memefactory.ui.get-dank.page
  (:require
   [clojure.string :as str]
   [district.ui.component.form.input :refer [index-by-type text-input with-label]]
   [district.ui.component.page :refer [page]]
   [memefactory.shared.utils :refer [tweet-url-regex]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.spinner :as spinner]
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
            succeeded? @(subscribe [:memefactory.ui.subs/dank-faucet-succeeded])]
        [app-layout
         {:meta {:title "MemeFactory - Receive initial DANK tokens"
                 :description "Make a tweet with your Ethereum address and we'll send you a one-time allotment of DANK tokens. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.get-dank-page
          [:div.get-dank-box
           [:div.icon]
           [:h2.title "Receive initial DANK tokens"]
           [:h3.title "Make a tweet with your Polygon address as shown below. Copy-paste the tweets URL into the above input box and we'll send you a one-time allotment of DANK tokens. Using your address instead, the content of the tweet should follow the format:"]
           [:h3.title "@MemeFactory0x $DANK 0x1234...aBcD"]
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

