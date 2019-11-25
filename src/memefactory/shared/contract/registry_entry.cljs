(ns memefactory.shared.contract.registry-entry
  (:require [bignumber.core :as bn]
            [clojure.set :as set]))

(def statuses
  {0 :reg-entry.status/challenge-period
   1 :reg-entry.status/commit-period
   2 :reg-entry.status/reveal-period
   3 :reg-entry.status/blacklisted
   4 :reg-entry.status/whitelisted})

(def load-registry-entry-keys [:reg-entry/deposit
                               :reg-entry/creator
                               :reg-entry/version
                               :challenge/challenger
                               :challenge/commit-period-end
                               :challenge/reveal-period-end
                               :challenge/reward-pool
                               :challenge/meta-hash
                               :challenge/claimed-reward-on])

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
