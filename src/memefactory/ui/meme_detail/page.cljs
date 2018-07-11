(ns memefactory.ui.meme-detail.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.ui.components.app-layout :as app-layout]
   [print.foo :refer [look] :include-macros true]   
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   ))

(defmethod page :route.meme-detail/index []
  (let [{:keys [:name :query :params]} @(re-frame/subscribe [::router-subs/active-page])
        active-account (subscribe [::accounts-subs/active-account])
        meme (subscribe [::gql/query {:queries [[:meme {:reg-entry/address (:address query)}
                                                 [:reg-entry/address
                                                  :reg-entry/status
                                                  :meme/image-hash
                                                  :meme/meta-hash
                                                  :meme/number
                                                  :meme/title
                                                  :meme/total-supply                                          
                                                  [:meme/owned-meme-tokens {:owner @active-account}
                                                   [:meme-token/token-id]]
                                                  [:reg-entry/creator [:user/address]]
                                                  
                                                  ]]]}])

        ]


    (prn @meme)
    
    [app-layout/app-layout
     {:meta {:title "MemeFactory"
             :description "Description"}}
     [:div.meme-detail {:style {:display "grid"
                                 :grid-template-areas
                                 "'image image image rank rank rank'
                                  "}}


      "MEME DETAIL"

      ]]))
