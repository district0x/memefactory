(ns memefactory.shared.contract.registry-entry
  (:require [bignumber.core :as bn]
            [cljs-web3.core :as web3]
            [clojure.set :as set]
            [district.web3-utils :refer [empty-address? wei->eth-number]]
            [memefactory.shared.utils :as shared-utils]))

(def statuses
  {0 :reg-entry.status/challenge-period
   1 :reg-entry.status/commit-period
   2 :reg-entry.status/reveal-period
   3 :reg-entry.status/blacklisted
   4 :reg-entry.status/whitelisted})

(def load-registry-entry-keys [:reg-entry/version
                               :reg-entry/status
                               :reg-entry/creator
                               :reg-entry/deposit
                               :reg-entry/challenge-period-end])

(def load-registry-entry-challenge-keys [:reg-entry/challenge-period-end
                                         :challenge/challenger
                                         :challenge/reward-pool
                                         :challenge/meta-hash
                                         :challenge/commit-period-end
                                         :challenge/reveal-period-end
                                         :challenge/votes-for
                                         :challenge/votes-against
                                         :challenge/claimed-reward-on
                                         :challenge/vote-quorum])

(def vote-props [:vote/secret-hash
                 :vote/option
                 :vote/amount
                 :vote/revealed-on
                 :vote/claimed-reward-on
                 :vote/reclaimed-deposit-on])

(def vote-options
  {0 :vote.option/no-vote
   1 :vote.option/vote-for
   2 :vote.option/vote-against})

(def vote-option->num (set/map-invert vote-options))

(defn parse-status [status]
  (statuses (bn/number status)))

(defn parse-load-registry-entry [reg-entry-addr registry-entry & [{:keys [:parse-dates?]}]]
  (when registry-entry
    (let [registry-entry (zipmap load-registry-entry-keys registry-entry)]
      (-> registry-entry
        (assoc :reg-entry/address reg-entry-addr)
        (update :reg-entry/version bn/number)
        (update :reg-entry/deposit wei->eth-number)
        (update :reg-entry/status parse-status)
        (update :reg-entry/challenge-period-end (constantly (shared-utils/parse-uint-date (:reg-entry/challenge-period-end registry-entry) parse-dates?)))))))

(defn parse-load-registry-entry-challenge [reg-entry-addr registry-entry & [{:keys [:parse-dates?]}]] 
  (when registry-entry
    (let [registry-entry (zipmap load-registry-entry-challenge-keys registry-entry)]
      (-> registry-entry
          (update :reg-entry/challenge-period-end (constantly (shared-utils/parse-uint-date (:reg-entry/challenge-period-end registry-entry) parse-dates?)))
          (update :challenge/challenger #(when-not (empty-address? %) %))
          (update :challenge/reward-pool wei->eth-number)
          (update :challenge/meta-hash #(when-not (empty-address? %) (web3/to-ascii %)))
          (update :challenge/commit-period-end (constantly (shared-utils/parse-uint-date (:challenge/commit-period-end registry-entry) parse-dates?)))
          (update :challenge/reveal-period-end (constantly (shared-utils/parse-uint-date (:challenge/reveal-period-end registry-entry) parse-dates?)))
          (update :challenge/votes-for bn/number)
          (update :challenge/vote-quorum bn/number)
          (update :challenge/votes-against bn/number)
          (update :challenge/claimed-reward-on (constantly (shared-utils/parse-uint-date (:challenge/claimed-reward-on registry-entry) parse-dates?)))))))

(defn parse-vote-option [vote-option]
  (vote-options (bn/number vote-option)))

(defn parse-load-vote [contract-addr voter-address voter & [{:keys [:parse-dates? :parse-vote-option?]}]]
  (when voter
    (let [voter (zipmap vote-props voter)]
      (-> voter
        (assoc :reg-entry/address contract-addr)
        (assoc :vote/voter voter-address)
        (update :vote/option (if parse-vote-option? parse-vote-option bn/number))
        (update :vote/amount bn/number)
        (update :vote/revealed-on (constantly (shared-utils/parse-uint-date (:vote/revealed-on voter) parse-dates?)))
        (update :vote/claimed-reward-on (constantly (shared-utils/parse-uint-date (:vote/claimed-reward-on voter) parse-dates?)))
        (update :vote/reclaimed-amount-on (constantly (shared-utils/parse-uint-date (:vote/reclaimed-deposit-on voter) parse-dates?)))))))
