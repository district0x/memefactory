(ns memefactory.ui.components.ens-resolver
  (:require
    [cljs-web3-next.core :as web3-core]
    [cljs-web3-next.eth :as web3-eth]
    [clojure.string :as string]
    [district.ui.logging.events :as logging]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.smart-contracts.subs :as contract-subs]
    [district.ui.web3.queries :as web3-queries]
    [district.web3-utils :as web3-utils]
    [memefactory.ui.utils :refer [l1-chain?]]
    [re-frame.core :as re-frame]
    [re-frame.core :refer [subscribe dispatch]])

  (:require-macros [reagent.ratom :refer [reaction]]))


;; This component is aimed to reverse-resolve addresses into names using the Ethereum Name Service (ENS).
;; For example, 0x12345...789.addr.reverse -> 'alice.eth'
;; (note we append ".addr.reverse" to the address to indicate this is a reverse resolution)
;;
;; ENS defines a Registry and Resolvers. Then, to make a reverse resolution we need to follow 4 steps.
;; 1) Query the ENS Registry to fetch the Resolver associated to the given address.
;;    This will give us the address of the Resolver (which holds the domain info of the address),
;;    if any associated.
;; 2) Query the Resolver obtained in the previous step to get the name associated to the address.
;;    If the address has a name associated, the resolver will give it to us and this would complete
;;    our resolution process.
;;    However, ENS does not enforce the accuracy of reverse records, resulting that anyone can claim
;;    that the name of their address is 'alice.eth'. Therefore, to be certain the claim is correct,
;;    we must perform a forward resolution to validate that the given name is resolved to the original
;;    address, thus requiring next steps.
;; 3) Query the ENS Registry to fetch the Resolver associted to the name obtained in previous step.
;;    That is similar to first step, but querying the name instead of the reverse address.
;; 4) Query the Resolver obtained in the previous step to get the address associated to the name.
;;    Now, if both the original address and the address the Resolver gives us match, we have
;;    accomplished the name resolution


(defn assoc-ens-name [db addr name]
  (assoc-in db [:memefactory.ui.components.ens-resolved-address (keyword addr)] name))


;; Subscription handler to fetch the name associated to a given address
(re-frame/reg-sub-raw
  ::ens-name
  (fn [db [_ addr]]
  (let [addr (string/lower-case addr)]
    (dispatch [::reverse-resolve-address addr])
    (reaction (get-in @db [:memefactory.ui.components.ens-resolved-address (keyword addr)])))))


(defn reverse-resolve
  "Convenience function to resolve an address to a name,
   showing the address until is resolved or if cannot be resolved at all.

   Parameters:
   addr       - the address to be reverse-resolved
  "
  [addr]
  (or (and (l1-chain?) @(subscribe [::contract-subs/contract-abi :ens]) ; make sure contract is already loaded
        (some? addr)
        @(subscribe [::ens-name addr]))
      addr))

;; Reduced version of the Resolver contract ABI for ENS addr/name resolution.
;; Note this is limited to addr() and name() methods
(def abi-resolver (js/JSON.parse "[{\"constant\":true,\"inputs\":[{\"name\":\"node\",\"type\":\"bytes32\"}],\"name\":\"addr\",\"outputs\":[{\"name\":\"ret\",\"type\":\"address\"}],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"node\",\"type\":\"bytes32\"}],\"name\":\"name\",\"outputs\":[{\"name\":\"ret\",\"type\":\"string\"}],\"payable\":false,\"type\":\"function\"}]"))

(defn namehash-join [node name]
  (subs (web3-core/sha3 (str node (subs (web3-core/sha3 name) 2)) ) 2))

(defn build-namehash [name]
  (str "0x" (reduce namehash-join (apply str (repeat 32 "00")) (rseq (string/split name #"\.")))))

(def interceptors [re-frame/trim-v])


;; Handler for (reverse-)resolving an address to a name.
;; Queries the ENS Registry to get the resolver associated to the given address
(re-frame/reg-event-fx
 ::reverse-resolve-address
 interceptors
 (fn [{:keys [:db]} [addr]]
  (let [instance (contract-queries/instance db :ens)
    namehash (build-namehash (str (string/lower-case (web3-utils/remove-0x addr)) ".addr.reverse" ))
    data {:addr addr :namehash namehash}]
    (when (not-empty (contract-queries/contract-abi db :ens))
      {:web3/call
        {:web3 (web3-queries/web3 db)
         :fns [{:instance instance
                :fn :resolver
                :args [namehash]
                :on-success [::resolver-get-address data]
                :on-error [::logging/error "Error calling ENS Registry" data ::reverse-resolve-address]}]}}))))


;; Handler for processing the response coming from the ENS registry, which indicates
;; the address of the resolver for the reverse address, if any.
;; It makes a query to the resolver to ask for the name of the address we want to reverse-resolve
(re-frame/reg-event-fx
  ::resolver-get-address
  interceptors
  (fn [{:keys [:db]} [{:keys [:addr :namehash] :as data} resolver-addr]]
    (when (not (web3-utils/empty-address? resolver-addr))
      (let [instance (web3-eth/contract-at (web3-queries/web3 db) abi-resolver resolver-addr)]
        {:web3/call
              {:web3 (web3-queries/web3 db)
               :fns [{:instance instance
                      :fn :name
                      :args [namehash]
                      :on-success [::validate-reverse-address data]
                      :on-error [::logging/error "Error calling ENS Resolver" (merge data {:resolver-addr resolver-addr}) ::resolver-get-address]}]}}))))


;; Handler for processing the response coming from the Resolver, which
;; indicates the name associated to a given (reverse) address, if any.
;; If the address is resolved to a name, it triggers a forward name resolution
;; to validate the name by querying the ENS Registry
(re-frame/reg-event-fx
 ::validate-reverse-address
 interceptors
 (fn [{:keys [:db]} [{:keys [:addr :namehash] :as data} name]]
  (when (not (string/blank? name))
    (let [instance (contract-queries/instance db :ens)
          namehash (build-namehash name)
          data {:name name :namehash namehash :original-addr addr}]
      {:web3/call
        {:web3 (web3-queries/web3 db)
         :fns [{:instance instance
                :fn :resolver
                :args [namehash]
                :on-success [::resolver-get-name data]
                :on-error [::logging/error "Error calling ENS Registry" data ::validate-reverse-address]}]}}))))


;; Handler for processing the response coming from the ENS Registry, which indicates
;; the address of the resolver associated to a given domain name, if any.
;; It makes a query to the speficied address to ask for the addres of the name want to forward-resolve
(re-frame/reg-event-fx
  ::resolver-get-name
  interceptors
  (fn [{:keys [:db]} [{:keys [:name :namehash :original-addr] :as data} resolver-addr]]
    (when (not (web3-utils/empty-address? resolver-addr))
      (let [instance (web3-eth/contract-at (web3-queries/web3 db) abi-resolver resolver-addr)]
        {:web3/call
          {:web3 (web3-queries/web3 db)
           :fns [{:instance instance
                  :fn :addr
                  :args [namehash]
                  :on-success [::validate-address-name data]
                  :on-error [::logging/error "Error calling ENS Resolver" (merge data {:resolver-addr resolver-addr}) ::resolver-get-name]}]}}))))


;; Handler for processing the response coming from the Resolver, which
;; indicates the address associated to a given name, if any.
;; It validates the forward-resolved address matches the original address
;; and sets the address-to-name relationship in the db to notify all
;; subscribers
(re-frame/reg-event-fx
  ::validate-address-name
  interceptors
  (fn [{:keys [:db]} [{:keys [:name :namehash :original-addr]} addr]]
    (when (= original-addr (string/lower-case addr))
      {:db (assoc-ens-name db addr name)})))
