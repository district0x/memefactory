(ns memefactory.ui.meme-detail.events
  (:require
   [cljs.spec.alpha :as s]
   [district.ui.graphql.events :as gql-events]
   [district.ui.logging.events :as logging]
   [district0x.re-frame.spec-interceptors :as spec-interceptors]
   [memefactory.ui.events.registry-entry :as registry-entry-events]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame :refer [reg-event-fx]]
   ))

(def interceptors [re-frame/trim-v])

(reg-event-fx
 ::claim-vote-reward
 [interceptors]
 (fn [{:keys [:db]} [{:keys [:tx-id :query-id :query :meme :vote/option :challenge/challenger :reg-entry/creator] :as args}]]
   {:async-flow {:first-dispatch [::registry-entry-events/claim-vote-reward {:send-tx/id tx-id
                                                                             :reg-entry/address (:reg-entry/address meme)
                                                                             :from (case option
                                                                                     :vote-option/vote-for (:user/address challenger)
                                                                                     :vote-option/vote-against (:user/address creator))}]
                 :rules [{:when :seen?
                          :events [::registry-entry-events/claim-vote-reward-success]
                          :dispatch [::gql-events/query {:query {:queries [query]}
                                                         :id query-id}]}]}}))
