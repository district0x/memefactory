(ns memefactory.shared.graphql-mock-resolvers)

(def graphql-mock-resolvers
  {:Query
   {:search-memes (fn [args context document]
                    {:total-count 1
                     :has-next-page true
                     :end-cursor "0x3f8cc2f7a4ed8c386b1339712366465c61073e8c",
                     :items
                     (constantly
                      [{:id "0x3f8cc2f7a4ed8c386b1339712366465c61073e8c",
                        :reg-entry/address "0x3f8cc2f7a4ed8c386b1339712366465c61073e8c",
                        :reg-entry/challenge-period-end 1520371829,
                        :reg-entry/created-on 1520371229,
                        :reg-entry/creator "0xd98afc1dd919ee845d9f07dd6e9f31a2d00c0866"
                        :reg-entry/deposit 1000,
                        :reg-entry/version 1,
                        :challenge/challenger "0xd98afc1dd919ee845d9f07dd6e9f31a2d00c0866",
                        :challenge/claimed-reward-on 0,
                        :challenge/commit-period-end 1520371349,
                        :challenge/created-on 1520371229,
                        :challenge/comment "Not a good meme",
                        :challenge/reveal-period-end 1520371409,
                        :challenge/reward-pool 500,
                        :challenge/votes-against 0,
                        :challenge/votes-for 9.99984e+26,
                        :challenge/votes-total 9.99984e+26,
                        :challenge/voting-token "0x8f450e039605ff48d32bcdab11dc111b98854577",
                        :meme/image-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH",
                        :meme/meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH",
                        :meme/offering-duration 0,
                        :meme/offering-start-price 929572691330084000,
                        :meme/offering-supply 2341,
                        :meme/offering-earnings 0
                        :meme/offering-rank 3
                        :meme/sold-for-total 0,
                        :meme/title "HapplyHarambe"
                        :meme/number 2
                        :meme/token "0xe1aeac8013624b928051ccec98517b6a455da68d",
                        :meme/total-supply 2342
                        :meme/tags [{:tag/name "Harambe"} {:tag/name "Someno Tag"}]
                        :challenge/vote (fn [{:keys [:vote/voter]}]
                                          {:id voter
                                           :vote/voter voter
                                           :vote/amount (rand-int 3000)})}])})
    :meme (fn [{:keys [:reg-entry/address]}]
            {:id address
             :reg-entry/address address
             :reg-entry/challenge-period-end 1520371829,
             :reg-entry/created-on 1520371229,
             :reg-entry/creator "0xd98afc1dd919ee845d9f07dd6e9f31a2d00c0866"
             :reg-entry/deposit 1000,
             :reg-entry/version 1,
             :challenge/challenger "0xd98afc1dd919ee845d9f07dd6e9f31a2d00c0866",
             :challenge/claimed-reward-on 0,
             :challenge/commit-period-end 1520371349,
             :challenge/created-on 1520371229,
             :challenge/comment "Not a good meme",
             :challenge/reveal-period-end 1520371409,
             :challenge/reward-pool 500,
             :challenge/votes-against 0,
             :challenge/votes-for 9.99984e+26,
             :challenge/votes-total 9.99984e+26,
             :challenge/voting-token "0x8f450e039605ff48d32bcdab11dc111b98854577",
             :meme/image-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH",
             :meme/meta-hash "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH",
             :meme/offering-duration 0,
             :meme/offering-start-price 929572691330084000,
             :meme/offering-supply 2341,
             :meme/offering-earnings 0
             :meme/offering-rank 3
             :meme/sold-for-total 0,
             :meme/title "HapplyHarambe"
             :meme/number 2
             :meme/token "0xe1aeac8013624b928051ccec98517b6a455da68d",
             :meme/total-supply 2342
             :meme/tags [{:tag/name "Harambe"} {:tag/name "Some Tag"}]})
    :param (fn [{:keys [:db :key]}]
             {:id key
              :param/key key
              :param/value (rand-int 999)
              :param/db db})
    :params (fn [{:keys [:db :keys]}]
              (for [key keys]
                {:id key
                 :param/key key
                 :param/value (rand-int 999)
                 :param/db db}))}})
