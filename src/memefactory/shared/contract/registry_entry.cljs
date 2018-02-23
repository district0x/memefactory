(ns memefactory.shared.contract.registry-entry
  (:require
    [bignumber.core :as bn]
    [clojure.set :as set]
    [district.web3-utils :refer [web3-time->local-date-time empty-address? wei->eth-number]]))

(def statuses
  {0 :reg-entry.status/challenge-period
   1 :reg-entry.status/commit-period
   2 :reg-entry.status/reveal-period
   3 :reg-entry.status/blacklisted
   4 :reg-entry.status/whitelisted})

(def load-result-props [:reg-entry/version
                        :reg-entry/status
                        :reg-entry/creator
                        :reg-entry/deposit
                        :reg-entry/challenge-period-end
                        :challenge/challenger
                        :challenge/voting-token
                        :challenge/reward-pool
                        :challenge/meta-hash
                        :challenge/commit-period-end
                        :challenge/reveal-period-end
                        :challenge/votes-for
                        :challenge/votes-against])

(def voter-props [:voter/secret-hash
                  :voter/vote-option
                  :voter/amount
                  :voter/revealed-on
                  :voter/claimed-reward-on])

(def vote-options
  {0 :vote-option/no-vote
   1 :vote-option/vote-for
   2 :vote-option/vote-against})

(def vote-option->num (set/map-invert vote-options))

(defn parse-status [status]
  (statuses (bn/number status)))

(defn parse-load-registry-entry [reg-entry-addr registry-entry & [{:keys [:parse-dates?]}]]
  (when registry-entry
    (let [registry-entry (zipmap load-result-props registry-entry)]
      (-> registry-entry
        (assoc :reg-entry/address reg-entry-addr)
        (update :reg-entry/version bn/number)
        (update :reg-entry/deposit wei->eth-number)
        (update :reg-entry/status parse-status)
        (update :reg-entry/challenge-period-end (if parse-dates? web3-time->local-date-time bn/number))
        (update :challenge/challenger #(when-not (empty-address? %) %))
        (update :challenge/voting-token #(when-not (empty-address? %) %))
        (update :challenge/reward-pool bn/number)
        (update :challenge/commit-period-end (if parse-dates? web3-time->local-date-time bn/number))
        (update :challenge/reveal-period-end (if parse-dates? web3-time->local-date-time bn/number))
        (update :challenge/votes-for bn/number)
        (update :challenge/votes-against bn/number)))))

(defn parse-vote-option [vote-option]
  (vote-options (bn/number vote-option)))

(defn parse-voter [voter-address voter & [{:keys [:parse-dates?]}]]
  (when voter
    (let [voter (zipmap voter-props voter)]
      (-> voter
        (assoc :voter/address voter-address)
        (update :voter/vote-option parse-vote-option)
        (update :voter/amount bn/number)
        (update :voter/revealed-on (if parse-dates? web3-time->local-date-time bn/number))
        (update :voter/claimed-reward-on (if parse-dates? web3-time->local-date-time bn/number))))))