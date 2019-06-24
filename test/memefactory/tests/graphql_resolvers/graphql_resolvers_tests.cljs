(ns memefactory.tests.graphql-resolvers.graphql-resolvers-tests
  (:require [cljs-time.core :as t]
            [cljs-time.coerce :as time-coerce]
            [cljs.test :refer-macros [deftest is testing use-fixtures run-tests]]
            [clojure.pprint :refer [pprint print-table]]
            [clojure.set :refer [difference]]
            [district.graphql-utils :as graphql-utils]
            [district.server.config :refer [config]]
            [district.server.db :as db]
            [district.server.graphql :as graphql]
            [district.server.graphql.utils :as utils]
            [district.server.logging :refer [logging]]
            [district.server.middleware.logging :refer [logging-middlewares]]
            [district.server.web3 :refer [web3]]
            [honeysql.core :as sql]
            [memefactory.server.db :as meme-db]
            [memefactory.server.generator]
            [memefactory.server.graphql-resolvers :as resolvers :refer [resolvers-map]]
            [memefactory.server.syncer]
            [memefactory.shared.graphql-schema :refer [graphql-schema]]
            [mount.core :as mount]
            [memefactory.server.utils :as server-utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Mock some data on the DB ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn hours->seconds [n]
  (* n 60 60))

(defn generate-some-data! []
  (let [now (server-utils/now-in-seconds)
        [in-commit-per in-reveal-per wl1 wl2 bl wl3 & r] (for [i (range 10)]
                                                           {:reg-entry/address (str "MEMEADDR" i)
                                                            :reg-entry/version 1
                                                            :reg-entry/creator (str "CADDR" i)
                                                            :reg-entry/deposit 1000
                                                            :reg-entry/created-on now
                                                            :meme/tags (cond
                                                                         (zero? (mod i 2)) ["tag1"]
                                                                         (zero? (mod i 3)) ["tag1" "tag2"]
                                                                         :else [])
                                                            :reg-entry/challenge-period-end (+ now (hours->seconds 1))
                                                            :meme/title (str "Meme Title " i)
                                                            :meme/image-hash (str "IHASH" i)
                                                            :meme/meta-hash (str "MHASH" i)
                                                            :meme/total-supply 5
                                                            :meme/total-minted 0
                                                            :meme/token-id-start 1})
        in-commit-per (assoc in-commit-per
                             :challenge/challenger "CHADDR"
                             :challenge/commit-period-end (+ now (hours->seconds 1))
                             :challenge/reveal-period-end (+ now (hours->seconds 2)))
        in-reveal-per (assoc in-reveal-per
                             :challenge/challenger "CHADDR"
                             :challenge/reveal-period-end (+ now (hours->seconds 2)))
        wl1 (assoc wl1 :reg-entry/challenge-period-end (dec now))
        wl2 (assoc wl2
                   :challenge/challenger "CHADDR"
                   :challenge/comment "This meme is a copy"
                   :challenge/created-on now
                   :challenge/commit-period-end (dec now)
                   :challenge/reveal-period-end (dec now)
                   :challenge/votes-for 1
                   :challenge/votes-against 0)
        wl3 (assoc wl3 :reg-entry/challenge-period-end (dec now))
        bl (assoc bl
                  :challenge/challenger "CHADDR"
                  :challenge/commit-period-end (dec now)
                  :challenge/reveal-period-end (dec now)
                  :challenge/votes-for 0
                  :challenge/votes-against 1)
        all-memes (into [in-commit-per in-reveal-per wl1 wl2 bl wl3] r)
        all-users (->> all-memes
                       (map (fn [m] {:user/address (:reg-entry/creator m)}))
                       (into #{{:user/address "BUYERADDR"}
                               {:user/address "VOTERADDR"
                                :user/voter-total-earned (+ (/ (:reg-entry/deposit wl2) 2)
                                                            (:reg-entry/deposit bl))}
                               {:user/address "CHADDR"
                                :user/challenger-total-earned (/ (:reg-entry/deposit wl2) 2)}}))
        votes (->> all-memes
                   (keep (fn [{:keys [:challenge/votes-for :challenge/votes-against :reg-entry/address]}]
                           (when (or votes-for votes-against)
                             {:reg-entry/address address
                              :vote/voter "VOTERADDR"
                              :vote/option (if (zero? votes-against) 1 0)
                              :vote/amount 1}))))
        tokens (->> [wl1 wl2 wl1 wl3]
                    (map-indexed (fn [token-id {:keys [:reg-entry/address :reg-entry/creator]}]
                                   {:meme-token/token-id token-id
                                    :reg-entry/address address
                                    ;; for passing down
                                    :meme-auction/seller creator})))
        auctions (let [duration (hours->seconds 10)]
                   (->> tokens
                        (map-indexed (fn [idx {:keys [:meme-token/token-id :meme-auction/seller]}]
                                       (merge
                                        {:meme-auction/address (str "AUCTIONADDR" idx)
                                         :meme-auction/token-id token-id
                                         :meme-auction/seller seller
                                         :meme-auction/start-price 100
                                         :meme-auction/description (str "Auction " idx " long description.")
                                         :meme-auction/end-price 50
                                         :meme-auction/started-on now
                                         :meme-auction/duration duration}
                                        (when (zero? (mod idx 2)) ;; make some buyed
                                          {:meme-auction/buyer "BUYERADDR"
                                           :meme-auction/bought-for (+ 70 idx)
                                           :meme-auction/bought-on (+ now duration)}))))))
        initial-params (let [k ["deposit" "challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration"]
                             v [1000 1e5 1e4 1e4]]
                         (for [i (range (count k))]
                           {:initial-param/key (get k i)
                            :initial-param/db "MEMEREGISTRYADDR"
                            :initial-param/value (get v i)
                            :initial-param/set-on now}))
        param-changes (let [v [2000 1e6 1e5 1e5 3000]]
                        (for [i (range (count v))]
                          {:reg-entry/address (str "PCHANGEADDR" i)
                           :reg-entry/version 1
                           :reg-entry/creator (str "CADDR" i)
                           :reg-entry/deposit 1000
                           :reg-entry/created-on now
                           :reg-entry/challenge-period-end (+ now (hours->seconds 1))
                           :param-change/db "MEMEREGISTRYADDR"
                           :param-change/key (get ["deposit" "challengePeriodDuration" "commitPeriodDuration" "revealPeriodDuration" "deposit"] i)
                           :param-change/initial-value (case i
                                                         0 (:initial-param/value (nth initial-params 0))
                                                         1 (:initial-param/value (nth initial-params 1))
                                                         2 (:initial-param/value (nth initial-params 2))
                                                         3 (:initial-param/value (nth initial-params 3))
                                                         4 (:initial-param/value (nth initial-params 0)))
                           :param-change/value (get v i)
                           :param-change/applied-on (+ now (hours->seconds i))}))]

    ;; Generate some users
    (doseq [u all-users]
      (meme-db/upsert-user! u))

    ;; Generate some memes
    (doseq [m all-memes]
      (meme-db/insert-registry-entry! m)
      (meme-db/insert-meme! m)
      (when-let [tags (:meme/tags m)]
        (doseq [t tags]
          (meme-db/tag-meme! {:reg-entry/address (:reg-entry/address m) :tag/name t}))))

    ;; Generate some votes
    (doseq [v votes]
      (meme-db/insert-vote! v))

    ;; Generate some meme-tokens
    (doseq [{:keys [:meme-token/token-id :reg-entry/address]} tokens]
      (meme-db/insert-meme-tokens! {:token-id-start token-id
                                    :token-id-end token-id
                                    :reg-entry/address address}))

    ;; Generate some meme-auctions
    (doseq [{:keys [:meme-auction/buyer :meme-auction/token-id] :as a} auctions]
      (meme-db/insert-meme-auction! a)
      (when buyer
        (meme-db/insert-or-replace-meme-token-owner {:meme-token/token-id token-id
                                                     :meme-token/owner buyer
                                                     :meme-token/transferred-on now})))

    ;; Generate some param-changes
    (doseq [pc param-changes]
      (meme-db/insert-registry-entry! pc)
      (meme-db/insert-param-change! pc))))

(defn after-fixture []
  (mount.core/stop #'memefactory.server.db/memefactory-db
                   #'district.server.graphql/graphql))

(defn before-fixture []
  ;; Just make sure everything is stopped
  (after-fixture)
  (-> (mount/with-args
        {:logging {:level "info"
                   :console? true}
         :web3 {:port 8549} ;; this is only needed because we are using blockchain timestamp
         :graphql {:port 6400
                   :middlewares [logging-middlewares]
                   :schema (utils/build-schema graphql-schema
                                               resolvers-map
                                               {:kw->gql-name graphql-utils/kw->gql-name
                                                :gql-name->kw graphql-utils/gql-name->kw})
                   :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                   :path "/graphql"
                   :graphiql true}
         :ranks-cache {:ttl (t/in-millis (t/minutes 60))}})
      (mount/except [#'district.server.smart-contracts/smart-contracts
                     #'memefactory.server.syncer/syncer])
      (mount.core/start))
  (generate-some-data!))



(use-fixtures :once {:before before-fixture :after after-fixture})

;;;;;;;;;;;
;; Tests ;;
;;;;;;;;;;;

(deftest meme-resolver-test
  (testing "Retriving meme contains all and not more data than requested"
    (is (= {:reg-entry/address "MEMEADDR3"
            :reg-entry/version 1
            :reg-entry/creator {:user/address "CADDR3"}
            :meme/title "Meme Title 3"}
           (-> (graphql/run-query {:queries [[:meme {:reg-entry/address "MEMEADDR3"}
                                              [:reg-entry/address
                                               :reg-entry/version
                                               :meme/title
                                               [:reg-entry/creator [:user/address]]]]]})
               :data :meme)))))

(deftest search-memes-test
  (testing "Search memes should retrieve nested values"
    (is (= {:total-count 10,
            :items [#:reg-entry{:address "MEMEADDR0",:creator #:user{:address "CADDR0"}}
                    #:reg-entry{:address "MEMEADDR1",:creator #:user{:address "CADDR1"}}
                    #:reg-entry{:address "MEMEADDR2",:creator #:user{:address "CADDR2"}}
                    #:reg-entry{:address "MEMEADDR3",:creator #:user{:address "CADDR3"}}
                    #:reg-entry{:address "MEMEADDR4",:creator #:user{:address "CADDR4"}}
                    #:reg-entry{:address "MEMEADDR5",:creator #:user{:address "CADDR5"}}
                    #:reg-entry{:address "MEMEADDR6",:creator #:user{:address "CADDR6"}}
                    #:reg-entry{:address "MEMEADDR7",:creator #:user{:address "CADDR7"}}
                    #:reg-entry{:address "MEMEADDR8",:creator #:user{:address "CADDR8"}}
                    #:reg-entry{:address "MEMEADDR9",:creator #:user{:address "CADDR9"}}]}
           (-> (graphql/run-query {:queries [[:search-memes
                                              [:total-count
                                               [:items [:reg-entry/address
                                                        [:reg-entry/creator [:user/address]]]]]]]})
               :data :search-memes))))

  (testing "Pagination should work"
    (let [{:keys [total-count has-next-page end-cursor items]}
          (-> (graphql/run-query {:queries [[:search-memes {:first 3}
                                             [:total-count
                                              :has-next-page
                                              :end-cursor
                                              [:items [:reg-entry/address]]]]]})
              :data :search-memes)]
      (is (= 10 total-count))
      (is has-next-page)
      (is (= "3" end-cursor))
      (is (= 3 (count items)))
      (is (= {:total-count 10,
              :has-next-page true,
              :end-cursor "6",
              :items
              [#:reg-entry{:address "MEMEADDR3"}
               #:reg-entry{:address "MEMEADDR4"}
               #:reg-entry{:address "MEMEADDR5"}]}
             (-> (graphql/run-query {:queries [[:search-memes {:first 3 :after "3"}
                                                [:total-count
                                                 :has-next-page
                                                 :end-cursor
                                                 [:items [:reg-entry/address]]]]]})
                 :data :search-memes)))))

  (testing "Filtering by statuses should work"
    (is (= {:items [#:reg-entry{:address "MEMEADDR0"}]}
           (-> (graphql/run-query {:queries [[:search-memes {:statuses [:reg-entry.status/commit-period]}
                                              [[:items [:reg-entry/address]]]]]})
               :data :search-memes))))

  (testing "Filtering by creator should work"
    (is (= {:items [#:reg-entry{:address "MEMEADDR7"}]}
           (-> (graphql/run-query {:queries [[:search-memes {:creator "CADDR7"}
                                              [[:items [:reg-entry/address]]]]]})
               :data :search-memes))))

  (testing "Filtering by curator should work"
    (is (= {:items [{:reg-entry/address "MEMEADDR3"} {:reg-entry/address "MEMEADDR4"}]})
        (-> (graphql/run-query {:queries [[:search-memes {:curator "VOTERADDR"}
                                           [[:items [:reg-entry/address]]]]]})
            :data :search-memes)))

  (testing "Sorting should work"
    (let [build-query (fn [order-dir] {:queries [[:search-memes {:order-by :memes.order-by/reveal-period-end :order-dir order-dir}
                                                  [[:items [:reg-entry/address]]]]]})]
      (is (= [#:reg-entry{:address "MEMEADDR2"}
              #:reg-entry{:address "MEMEADDR5"}
              #:reg-entry{:address "MEMEADDR6"}
              #:reg-entry{:address "MEMEADDR7"}
              #:reg-entry{:address "MEMEADDR8"}
              #:reg-entry{:address "MEMEADDR9"}
              #:reg-entry{:address "MEMEADDR3"}
              #:reg-entry{:address "MEMEADDR4"}
              #:reg-entry{:address "MEMEADDR0"}
              #:reg-entry{:address "MEMEADDR1"}]
             (-> (graphql/run-query (build-query :asc))
                 :data :search-memes :items))))))

(deftest search-meme-tokens-test
  (testing "Should retrieve meme tokens"
    (is (= (-> (graphql/run-query {:queries [[:search-meme-tokens
                                              [:total-count
                                               :has-next-page
                                               :end-cursor
                                               [:items [:meme-token/token-id]]]]]})
               :data :search-meme-tokens)
           {:total-count 4,
            :has-next-page false,
            :end-cursor "4",
            :items [#:meme-token{:token-id "0"} #:meme-token{:token-id "1"} #:meme-token{:token-id "2"} #:meme-token{:token-id "3"}]})))

  (testing "Filter by owner should work"
    (is (= 2
           (-> (graphql/run-query {:queries [[:search-meme-tokens {:owner "BUYERADDR"}
                                              [:total-count]]]})
               :data :search-meme-tokens :total-count)))
    (is (= 0
           (-> (graphql/run-query {:queries [[:search-meme-tokens {:owner "CADDR7"}
                                              [:total-count]]]})
               :data :search-meme-tokens :total-count))))

  (testing "Sorting by Meme Title should work"
    (let [build-query (fn [order] {:queries [[:search-meme-tokens {:order-by :meme-tokens.order-by/meme-title :order-dir order}
                                              [[:items [[:meme-token/meme [:meme/title]]]]]]]})]
      (is (= (-> (graphql/run-query (build-query :asc))
                 :data :search-meme-tokens :items)
             (-> (graphql/run-query (build-query :desc))
                 :data :search-meme-tokens :items
                 reverse))))))

(deftest search-meme-auctions-test
  (testing "Should retrieve meme auctions"
    (is (= {:total-count 4,
            :end-cursor "4",
            :has-next-page false,
            :items [#:meme-auction{:address "AUCTIONADDR0"}
                    #:meme-auction{:address "AUCTIONADDR1"}
                    #:meme-auction{:address "AUCTIONADDR2"}
                    #:meme-auction{:address "AUCTIONADDR3"}]}
           (-> (graphql/run-query {:queries [[:search-meme-auctions
                                              [:total-count
                                               :end-cursor
                                               :has-next-page
                                               [:items [:meme-auction/address]]]]]})
               :data :search-meme-auctions))))

  #_(testing "Sorting by price should work"
    (is (= [#:meme-auction{:address "AUCTIONADDR2"}
            #:meme-auction{:address "AUCTIONADDR0"}
            #:meme-auction{:address "AUCTIONADDR1"}
            #:meme-auction{:address "AUCTIONADDR3"}]
           (-> (graphql/run-query {:queries [[:search-meme-auctions {:order-by :meme-auctions.order-by/price :order-dir :desc}
                                              [[:items [:meme-auction/address]]]]]})
               :data :search-meme-auctions :items))))

  (testing "Filter by title should work"
    (is (= [{:meme-auction/meme-token {:meme-token/meme {:meme/title "Meme Title 3"}}}]
           (->> (graphql/run-query {:queries [[:search-meme-auctions {:title "Meme Title 3"}
                                               [[:items [[:meme-auction/meme-token [[:meme-token/meme [:meme/title]]]]]]]]]})
                :data :search-meme-auctions :items))))

  #_(testing "Grouping should work"
    (is (= [#:meme-auction{:bought-for 70,:meme-token #:meme-token{:meme #:meme{:title "Meme Title 2"}}}
            #:meme-auction{:bought-for nil,:meme-token #:meme-token{:meme #:meme{:title "Meme Title 3"}}}
            #:meme-auction{:bought-for nil,:meme-token #:meme-token{:meme #:meme{:title "Meme Title 5"}}}]
           (->> (graphql/run-query {:queries [[:search-meme-auctions {:group-by :meme-auctions.group-by/cheapest}
                                               [[:items [:meme-auction/bought-for
                                                         [:meme-auction/meme-token [[:meme-token/meme [:meme/title]]]]]]]]]})
                :data :search-meme-auctions :items))))

  (testing "Filter by tags-or should work"
    (->> (graphql/run-query {:queries [[:search-meme-auctions {:tags-or ["tag2"]}
                                        [[:items [:meme-auction/address
                                                  [:meme-auction/meme-token [[:meme-token/meme [:meme/title
                                                                                                :reg-entry/address
                                                                                                [:meme/tags [:tag/name]]]]]]]]]]]})
         :data :search-meme-auctions :items))

  (testing "Filter by tags should work"
    (is (= [{:meme-auction/address "AUCTIONADDR1",
             :meme-auction/meme-token {:meme-token/meme {:meme/title "Meme Title 3",
                                                         :reg-entry/address "MEMEADDR3",
                                                         :meme/tags [#:tag{:name "tag1"}
                                                                     #:tag{:name "tag2"}]}}}]
           (->> (graphql/run-query {:queries [[:search-meme-auctions {:tags ["tag1" "tag2"]}
                                               [[:items [:meme-auction/address
                                                         [:meme-auction/meme-token [[:meme-token/meme [:meme/title
                                                                                                       :reg-entry/address
                                                                                                       [:meme/tags [:tag/name]]]]]]]]]]]})
                :data :search-meme-auctions :items)))))

(deftest meme-auction-test
  (testing "Should retrieve meme auction"
    (is (= [#:meme-auction{:address "AUCTIONADDR1", :start-price 100}]
           [(-> (graphql/run-query {:queries [[:meme-auction {:meme-auction/address "AUCTIONADDR1"}
                                               [:meme-auction/address
                                                :meme-auction/start-price]]]})
                :data :meme-auction)]))))

(deftest search-tags-test
  (testing "Should retrieve tags"
    (is (= {:total-count 2, :items [#:tag{:name "tag1"} #:tag{:name "tag2"}]}
           (-> (graphql/run-query {:queries [[:search-tags
                                              [:total-count
                                               [:items [:tag/name]]]]]})
               :data :search-tags)))))

(deftest meme->owned-meme-tokens-test
  (testing "Retrieving a meme with owned meme tokens should work"
    (is (= {:meme/owned-meme-tokens [{:meme-token/token-id "0"} {:meme-token/token-id "2"}]}
           (-> (graphql/run-query {:queries [[:meme {:reg-entry/address "MEMEADDR2"}
                                              [[:meme/owned-meme-tokens {:owner "BUYERADDR"} [:meme-token/token-id]]]]]})
               :data :meme)))))

(deftest param-change-test
  (testing "Should retrieve param change"
    (is (= {:reg-entry/address "PCHANGEADDR0",
            :param-change/db "MEMEREGISTRYADDR",
            :param-change/key "deposit",
            :param-change/value 2000}
           (-> (graphql/run-query {:queries [[:param-change {:reg-entry/address "PCHANGEADDR0"}
                                              [:reg-entry/address
                                               :param-change/db
                                               :param-change/key
                                               :param-change/value]]]})
               :data :param-change)))))

(deftest search-param-changes-test
  (testing "Search params should retrieve paginated values"
    (is (= {:total-count 2,
            :end-cursor "2",
            :items [#:param-change{:db "MEMEREGISTRYADDR" :key "deposit" :value 2000}
                    #:param-change{:db "MEMEREGISTRYADDR" :key "deposit" :value 3000}]}
           (-> (graphql/run-query {:queries [[:search-param-changes {:key "deposit"
                                                                     :db "MEMEREGISTRYADDR"}
                                              [:total-count
                                               :end-cursor
                                               [:items [:param-change/db
                                                        :param-change/key
                                                        :param-change/value]]]]]})
               :data :search-param-changes))))

  (testing "Search params order-by abd group-by can be used to retrieve most recent parameter values"
    (is (= {:total-count 1,
            :end-cursor "1",
            :items [#:param-change{:db "MEMEREGISTRYADDR" :key "deposit" :value 3000}]}
           (-> (graphql/run-query {:queries [[:search-param-changes {:key "deposit" :db "MEMEREGISTRYADDR"
                                                                     :group-by :param-changes.group-by/key
                                                                     :order-by :param-changes.order-by/applied-on
                                                                     :order-dir :asc}
                                              [:total-count
                                               :end-cursor
                                               [:items [:param-change/db
                                                        :param-change/key
                                                        :param-change/value]]]]]})
               :data :search-param-changes)))))

(deftest user-test
  (testing "Test non existing user is nil"
    (is (nil? (-> (graphql/run-query {:queries [[:user {:user/address "FOOBAR"}
                                                 [:user/address]]]})
                  :data
                  :user))))
  (testing "Test total-created-memes & total-created-memes-whitelisted"
    (is (= [#:user{:address "CADDR2"
                   :total-created-memes 1
                   :total-created-memes-whitelisted 1}]
           [(-> (graphql/run-query {:queries [[:user {:user/address "CADDR2"}
                                               [:user/address
                                                :user/total-created-memes
                                                :user/total-created-memes-whitelisted]]]})
                :data
                :user)])))
  (testing "Test total-collected-token-ids & total-collected-memes"
    (is (= [#:user{:address "BUYERADDR"
                   :total-collected-token-ids 2
                   :total-collected-memes 1}]
           [(-> (graphql/run-query {:queries [[:user {:user/address "BUYERADDR"}
                                               [:user/address
                                                :user/total-collected-token-ids
                                                :user/total-collected-memes]]]})
                :data
                :user)])))
  (testing "Test total-created-challenges & total-created-challenges-success"
    (is (= [#:user{:address "CHADDR"
                   :total-created-challenges 4
                   :total-created-challenges-success 1}]
           [(-> (graphql/run-query {:queries [[:user {:user/address "CHADDR"}
                                               [:user/address
                                                :user/total-created-challenges
                                                :user/total-created-challenges-success]]]})
                :data
                :user)])))
  (let [largest-sale-query [[:user {:user/address "CADDR2"}
                             [:user/address
                              [:user/largest-sale [:meme-auction/address
                                                   :meme-auction/bought-for
                                                   [:meme-auction/buyer [:user/address]]
                                                   [:meme-auction/seller [:user/address]]]]]]]
        creator-largest-sale-query [[:user {:user/address "CADDR2"}
                                     [:user/address
                                      [:user/creator-largest-sale [:meme-auction/address
                                                                   :meme-auction/bought-for
                                                                   [:meme-auction/buyer [:user/address]]
                                                                   [:meme-auction/seller [:user/address]]]]]]]
        largest-sale #:user{:address "CADDR2",
                            :largest-sale #:meme-auction{:address "AUCTIONADDR2",
                                                         :bought-for 72,
                                                         :buyer #:user{:address "BUYERADDR"},
                                                         :seller #:user{:address "CADDR2"}}}
        creator-largest-sale #:user{:address "CADDR2",
                                    :creator-largest-sale #:meme-auction{:address "AUCTIONADDR2",
                                                                         :bought-for 72,
                                                                         :buyer #:user{:address "BUYERADDR"},
                                                                         :seller #:user{:address "CADDR2"}}}]
    (testing "Test largest-sale"
      (is (= largest-sale
             (-> (graphql/run-query {:queries largest-sale-query})
                 :data
                 :user))))
    (testing "Test creator-largest-sale"
      (is (= creator-largest-sale
             (-> (graphql/run-query {:queries creator-largest-sale-query})
                 :data
                 :user))))))

(deftest search-users-test
  (testing "Test order-by total-collected-memes"
    (is (= {:total-count 13
            :items [#:user{:address "BUYERADDR"
                           :total-collected-memes 1}]}
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-collected-memes
                                                             :order-dir :desc
                                                             :first 1
                                                             :after "0"}
                                              [:total-count
                                               [:items [:user/address
                                                        :user/total-collected-memes]]]]]})
               :data :search-users))))
  (testing "Test order-by total-created-memes"
    (is (= [#:user{:address "CADDR6", :total-created-memes 1}
            #:user{:address "CADDR2", :total-created-memes 1}
            #:user{:address "CADDR3", :total-created-memes 1}
            #:user{:address "CADDR7", :total-created-memes 1}
            #:user{:address "CADDR1", :total-created-memes 1}
            #:user{:address "CADDR8", :total-created-memes 1}
            #:user{:address "CADDR9", :total-created-memes 1}
            #:user{:address "CADDR0", :total-created-memes 1}
            #:user{:address "CADDR4", :total-created-memes 1}
            #:user{:address "CADDR5", :total-created-memes 1}
            #:user{:address "BUYERADDR", :total-created-memes 0}
            #:user{:address "CHADDR", :total-created-memes 0}
            #:user{:address "VOTERADDR", :total-created-memes 0}]
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-created-memes
                                                             :order-dir :desc}
                                              [[:items [:user/address
                                                        :user/total-created-memes]]]]]})
               :data :search-users :items))))
  (testing "Test order-by total-created-memes-whitelisted"
    (is (= [{:user/address "CADDR2", :user/total-created-memes-whitelisted 1}
            {:user/address "CADDR3", :user/total-created-memes-whitelisted 1}
            {:user/address "CADDR5", :user/total-created-memes-whitelisted 1}
            {:user/address "CADDR6", :user/total-created-memes-whitelisted 0}
            {:user/address "BUYERADDR", :user/total-created-memes-whitelisted 0}
            {:user/address "CADDR7", :user/total-created-memes-whitelisted 0}
            {:user/address "CADDR1", :user/total-created-memes-whitelisted 0}
            {:user/address "CADDR8", :user/total-created-memes-whitelisted 0}
            {:user/address "CHADDR", :user/total-created-memes-whitelisted 0}
            {:user/address "CADDR9", :user/total-created-memes-whitelisted 0}
            {:user/address "CADDR0", :user/total-created-memes-whitelisted 0}
            {:user/address "CADDR4", :user/total-created-memes-whitelisted 0}
            {:user/address "VOTERADDR", :user/total-created-memes-whitelisted 0}]
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-created-memes-whitelisted
                                                             :order-dir :desc}
                                              [[:items [:user/address
                                                        :user/total-created-memes-whitelisted]]]]]})
               :data :search-users :items))))
  (testing "Test order-by total-collected-token-ids"
    (is (= {:items [#:user{:address "BUYERADDR"
                           :total-collected-token-ids 2}]}
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-collected-token-ids
                                                             :order-dir :desc
                                                             :first 1
                                                             :after "0"}
                                              [[:items [:user/address
                                                        :user/total-collected-token-ids]]]]]})
               :data :search-users))))
  (testing "Test order-by total-created-challenges"
    (is (= {:items [#:user{:address "CHADDR"
                           :total-created-challenges 4}]}
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-created-challenges
                                                             :order-dir :desc
                                                             :first 1
                                                             :after "0"}
                                              [[:items [:user/address
                                                        :user/total-created-challenges]]]]]})
               :data :search-users))))
  (testing "Test order-by total-created-challenges-success"
    (is (= {:items [#:user{:address "CHADDR"
                           :total-created-challenges-success 1}]}
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-created-challenges-success
                                                             :order-dir :desc
                                                             :first 1
                                                             :after "0"}
                                              [[:items [:user/address
                                                        :user/total-created-challenges-success]]]]]})
               :data :search-users))))
  (testing "Test order-by total-participated-votes"
    (is (= {:items [#:user{:address "VOTERADDR"
                           :total-participated-votes 2}]}
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-participated-votes
                                                             :order-dir :desc
                                                             :first 1
                                                             :after "0"}
                                              [[:items [:user/address
                                                        :user/total-participated-votes]]]]]})
               :data :search-users))))
  (testing "Test order-by total-participated-votes-success"
    (is (= {:items [#:user{:address "VOTERADDR"
                           :total-participated-votes-success 1}]}
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/total-participated-votes-success
                                                             :order-dir :desc
                                                             :first 1
                                                             :after "0"}
                                              [[:items [:user/address
                                                        :user/total-participated-votes-success]]]]]})
               :data :search-users))))
  (testing "Test order-by curator-total-earned"
    (is (= {:items [#:user{:address "VOTERADDR"
                           :user/curator-total-earned 1500}]}
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/curator-total-earned
                                                             :order-dir :desc
                                                             :first 1
                                                             :after "0"}
                                              [[:items [:user/address
                                                        :user/curator-total-earned]]]]]})
               :data :search-users))))
  (testing "Test order-by user-address"
    (is (= [#:user{:address "BUYERADDR"}
            #:user{:address "CADDR0"}
            #:user{:address "CADDR1"}
            #:user{:address "CADDR2"}
            #:user{:address "CADDR3"}
            #:user{:address "CADDR4"}
            #:user{:address "CADDR5"}
            #:user{:address "CADDR6"}
            #:user{:address "CADDR7"}
            #:user{:address "CADDR8"}
            #:user{:address "CADDR9"}
            #:user{:address "CHADDR"}
            #:user{:address "VOTERADDR"}]
           (-> (graphql/run-query {:queries [[:search-users {:order-by :users.order-by/address
                                                             :order-dir :asc
                                                             :first 13
                                                             :after "0"}
                                              [[:items [:user/address]]]]]})
               :data :search-users :items)))))

(deftest param-test
  (testing "Should retrieve param"
    (is (= [#:param{:db "MEMEREGISTRYADDR", :key "deposit", :value 2000}]
           [(-> (graphql/run-query {:queries [[:param {:db "MEMEREGISTRYADDR" :key "deposit"}
                                               [:param/db
                                                :param/key
                                                :param/value]]]})
                :data :param)]))))

(deftest params-test
  #_(testing "Should retrieve params collection"
    (is (empty? (difference #{{:param/value 1e5} {:param/value 2000} {:param/value 3000}}
                            (set (-> (graphql/run-query {:queries [[:params {:db "MEMEREGISTRYADDR" :keys ["commitPeriodDuration" "deposit"]}
                                                                    [:param/value]]]})
                                     :data :params)))))))

(deftest ranks-test
  #_(testing "Should retrieve ranks correctly"
    (is (= [#:user{:creator-rank 12,
                   :challenger-rank 11,
                   :curator-rank 0,
                   :voter-rank 0}]
           [(-> (graphql/run-query {:queries [[:user {:user/address "VOTERADDR"}
                                               [:user/creator-rank
                                                :user/challenger-rank
                                                :user/curator-rank
                                                :user/voter-rank]]]})
                :data :user)]))))

(comment
  (do (after-fixture)
      (before-fixture))
  (run-tests))
