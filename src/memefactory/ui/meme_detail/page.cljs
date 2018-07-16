(ns memefactory.ui.meme-detail.page
  (:require
   [cljs-web3.core :as web3]
   [district.format :as format]
   [memefactory.ui.utils :as ui-utils]
   [district.graphql-utils :as graphql-utils]
   [print.foo :refer [look] :include-macros true]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.ui.components.app-layout :as app-layout]
   [print.foo :refer [look] :include-macros true]   
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [memefactory.ui.components.tiles :as tiles]
   ))

(def description "Lorem ipsum dolor sit amet, consectetur adipiscing elit")


(defn meme-creator [{:keys [:user/address :user/creator-rank :user/total-created-memes
                                         :user/total-created-memes-whitelisted] :as creator}]
  (let [query (subscribe [::gql/query
                          {:queries [[:search-meme-auctions {:seller address :statuses [:meme-auction.status/done]}
                                      [[:items [;;:meme-auction/start-price
                                                :meme-auction/end-price
                                                ;;:meme-auction/bought-for
                                                ]]]]
                                     #_[:user {:user/address address}
                                        [:user/total-created-memes
                                         :user/total-created-memes-whitelisted
                                         :user/creator-rank
                                         [:user/largest-sale [:meme-auction/start-price
                                                              :meme-auction/end-price
                                                              :meme-auction/bought-for
                                                              [:meme-auction/meme-token
                                                               [:meme-token/number
                                                                [:meme-token/meme
                                                                 [:meme/title]]]]]]]]]}])
        creator-total-earned (reduce (fn [total-earned {:keys [:meme-auction/end-price] :as meme-auction}]
                                       (+ total-earned end-price))
                                     0
                                     (-> @query :search-meme-auctions :items))]
    
    (when-not (:graphql/loading? @query)
      [:div.creator
       [:b "Creator"]
       [:div.rank (str "Rank: #" creator-rank " (" (format/format-eth (web3/from-wei creator-total-earned :ether)) ")")]       
       [:div.success (str "Success rate: " total-created-memes-whitelisted "/" total-created-memes " ("
                          (format/format-percentage total-created-memes-whitelisted total-created-memes) ")")]
       [:div.address (str "Address: " address)]])))

(defmethod page :route.meme-detail/index []
  (let [
        {:keys [:name :query :params]} @(re-frame/subscribe [::router-subs/active-page])
        active-account (subscribe [::accounts-subs/active-account])
        meme (subscribe [::gql/query {:queries [[:meme {:reg-entry/address (:address query)}
                                                 [;;:reg-entry/address
                                                  :reg-entry/status
                                                  :meme/image-hash
                                                  :meme/meta-hash
                                                  :meme/number
                                                  :meme/title
                                                  :meme/total-supply                                          
                                                  [:meme/owned-meme-tokens {:owner @active-account}
                                                   [:meme-token/token-id]]
                                                  [:reg-entry/creator
                                                   [:user/address
                                                    :user/total-created-memes
                                                    :user/total-created-memes-whitelisted
                                                    :user/creator-rank
                                                    ]]]]
                                                [:search-tags [[:items [:tag/name]]]]]}])]

;;tags (subscribe [::gql/query {:queries []}])
    
    (when-not (:graphql/loading? @meme)

      (prn @meme)
      
      (if-let [{:keys [:meme/image-hash :meme/title :reg-entry/status :meme/total-supply
                       :meme/owned-meme-tokens :reg-entry/creator]} (:meme @meme)] 
        (let [;;{:keys [:user/address :user/creator-rank]} creator
              token-count (->> owned-meme-tokens
                              (map :meme-token/token-id)
                              (filter ui-utils/not-nil?)
                              count)]

          #_(prn (->> owned-meme-tokens
                    (map :meme-token/token-id)))
          
        [app-layout/app-layout
                 {:meta {:title "MemeFactory"
                         :description "Description"}}
                 [:div.meme-detail {:style {:display "grid"
                                            :grid-template-areas
                                            "'image image image rank rank rank'
                                  "}}

                  [:div {:style {:grid-area "image"}}
                   [tiles/meme-image image-hash]]

                  [:div {:style {:grid-area "rank"}}
                   [:div.title [:h1 title]]
                   [:div.status (case (graphql-utils/gql-name->kw status)
                                  :reg-entry.status/whitelisted [:label "In Registry"]
                                  :reg-entry.status/blacklisted [:label "Rejected"]
                                  [:label "Challenged"])]
                   [:div.description description]
                   [:div.text (str total-supply " cards") ]
                   [:div.text (str "You own " token-count) ]

                   [meme-creator creator]

                   ;; TODO: tags

                   ]

                  ]])))))
