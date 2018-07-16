(ns memefactory.ui.meme-detail.page
  (:require
;;   [district.time :as time]
   [district.ui.now.subs :as now-subs]
   [district.ui.router.events :as router-events]
   [cljs-web3.core :as web3]
   [district.format :as format]
   [memefactory.ui.utils :as ui-utils]
   [memefactory.shared.utils :as shared-utils]
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

;; TODO :order-by
(defn history [address]
  (let [now (subscribe [::now-subs/now])
        query (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                 [;;:reg-entry/address
                                                  ;; :reg-entry/status
                                                  ;; :meme/image-hash
                                                  ;; :meme/meta-hash
                                                  ;; :meme/number
                                                  ;; :meme/title
                                                  ;; :meme/total-supply                                         
                                                  
                                                  #_[:meme/owned-meme-tokens {:owner @active-account}
                                                   [:meme-token/token-id]]

                                                  #_[:reg-entry/creator
                                                   [:user/address
                                                    :user/total-created-memes
                                                    :user/total-created-memes-whitelisted
                                                    :user/creator-rank
                                                    ]]

                                                  [:meme/meme-auctions #_{:order-by "order-by" :order-dir "order-dir"}
                                                   [:meme-auction/address
                                                    :meme-auction/end-price
                                                    :meme-auction/bought-on
                                                    [:meme-auction/seller
                                                     [:user/address]]
                                                    [:meme-auction/buyer
                                                     [:user/address]]
                                                    [:meme-auction/meme-token
                                                     [:meme-token/token-id]]

                                                    ]]
                                                  
                                                  ]]
                                                 ]}])]
   
    (when-not (:graphql/loading? @query)
      
      (prn @query)
      
      [:div.title [:h1 "Marketplace history"]]

      [:table
       [:thead [:tr
                [:th {:on-click #(prn "TODO: sort by tokrn-id")} "Card Number"]
                [:th {:on-click #(prn "TODO: sort by seller")} "Seller"]
                [:th {:on-click #(prn "TODO: sort by buyer")} "Buyer"]
                [:th {:on-click #(prn "TODO: sort by proce")} "Price"]
                [:th {:on-click #(prn "TODO: sort by bough-on")} "Time Ago"]]]
       [:tbody (doall
                (for [{:keys [:meme-auction/address :meme-auction/end-price :meme-auction/bought-on
                              :meme-auction/meme-token :meme-auction/seller :meme-auction/buyer] :as auction} (-> @query :meme :meme/meme-auctions)]

                  (do

                    (prn bought-on)
                    ;;(prn  (format/time-ago bought-on @now))

                    
                    ^{:key address}
                    [:tr
                     #_[:th {:scope "row"} (inc index)]
                     [:td (:meme-token/token-id meme-token)]
                     [:td (:user/address seller)]
                     [:td (:user/address buyer)]
                     [:td end-price]
                     ;; TODO: format to time-ago
                     [:td "bought-on"]

                     ])))]]))

  )

;;  challenge related
(defmethod page :route.meme-detail/index []
  (let [{:keys [:name :query :params]} @(re-frame/subscribe [::router-subs/active-page])
        {:keys [:address]} query
        active-account (subscribe [::accounts-subs/active-account])
        query (subscribe [::gql/query {:queries [[:meme {:reg-entry/address address}
                                                 [
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
                                                    ]]

                                                  [:meme/meme-auctions
                                                   [:meme-auction/address]]
                                                  
                                                  ]]
                                                [:search-tags [[:items [:tag/name]]]]]}])]

;;tags (subscribe [::gql/query {:queries []}])
    
    (when-not (:graphql/loading? @query)

;;      (prn @query)
      
      (if-let [{:keys [:meme/image-hash :meme/title :reg-entry/status :meme/total-supply
                       :meme/owned-meme-tokens :reg-entry/creator :meme/meme-auctions]} (:meme @query)] 
        (let [;;tags (->> @query :search-tags :items (mapv :tag/name)) ;;{:keys [:user/address :user/creator-rank]} creator
              token-count (->> owned-meme-tokens
                              (map :meme-token/token-id)
                              (filter shared-utils/not-nil?)
                              count)
              ]
          
        [app-layout/app-layout
                 {:meta {:title "MemeFactory"
                         :description "Description"}}

                    [:div.meme-detail {:style {:display "grid"
                                            :grid-template-areas
                                            "'image image image rank rank rank'
                                             'history history history history history history'"}}

                     ;; meme
                     
                     [:div {:style {:grid-area "image"}}
                      [tiles/meme-image image-hash]]

                     [:div {:style {:grid-area "rank"}}
                      [:div.title [:h1 title]]
                      [:div.status (case (graphql-utils/gql-name->kw status)
                                     :reg-entry.status/whitelisted [:label "In Registry"]
                                     :reg-entry.status/blacklisted [:label "Rejected"]
                                     [:label "Challenged"])]
                      [:div.description description]
                      [:div.text (format/pluralize total-supply "card") ]
                      [:div.text (str "You own " token-count) ]

                      [meme-creator creator]

                      ;; TODO: buttons and search to marketplace
                      (for [tag-name (->> @query :search-tags :items (mapv :tag/name))]
                        ^{:key tag-name} [:div {:style {:display "inline"}} tag-name])

                      [:div.buttons
                       [:button {:on-click #(dispatch [::router-events/navigate
                                                       :route.marketplace/index
                                                       nil
                                                       {:term title}])} "Search On Marketplace"]
                       [:button {:on-click #(dispatch [::router-events/navigate
                                                       :route.memefolio/index
                                                       nil
                                                       {:term title}])} "Search On Memefolio"]]]

                  
                  ;; history
                  [:div.history {:style {:grid-area "history"}}
                   [history address]]
                  

                  ]
         ])))))
