(ns memefactory.server.constants)

(def web3-events
  {:meme-registry-db/eternal-db-event [:meme-registry-db :EternalDbEvent]
   :meme-registry/meme-constructed-event [:meme-registry-fwd :MemeConstructedEvent]
   :meme-registry/challenge-created-event [:meme-registry-fwd :ChallengeCreatedEvent]
   :meme-registry/vote-committed-event [:meme-registry-fwd :VoteCommittedEvent]
   :meme-registry/vote-revealed-event [:meme-registry-fwd :VoteRevealedEvent]
   :meme-registry/vote-amount-claimed-event [:meme-registry-fwd :VoteAmountClaimedEvent]
   :meme-registry/vote-reward-claimed-event [:meme-registry-fwd :VoteRewardClaimedEvent]
   :meme-registry/challenge-reward-claimed-event [:meme-registry-fwd :ChallengeRewardClaimedEvent]
   :meme-registry/meme-minted-event [:meme-registry-fwd :MemeMintedEvent]
   :meme-auction-factory/meme-auction-started-event [:meme-auction-factory-fwd :MemeAuctionStartedEvent]
   :meme-auction-factory/meme-auction-buy-event [:meme-auction-factory-fwd :MemeAuctionBuyEvent]
   :meme-auction-factory/meme-auction-canceled-event [:meme-auction-factory-fwd :MemeAuctionCanceledEvent]
   :meme-token/transfer [:meme-token :Transfer]
   :param-change-db/eternal-db-event [:param-change-registry-db :EternalDbEvent]
   :param-change-registry/param-change-constructed-event [:param-change-registry-fwd :ParamChangeConstructedEvent]
   :param-change-registry/challenge-created-event [:param-change-registry-fwd :ChallengeCreatedEvent]
   :param-change-registry/vote-committed-event [:param-change-registry-fwd :VoteCommittedEvent]
   :param-change-registry/vote-revealed-event [:param-change-registry-fwd :VoteRevealedEvent]
   :param-change-registry/vote-amount-claimed-event [:param-change-registry-fwd :VoteAmountClaimedEvent]
   :param-change-registry/vote-reward-claimed-event [:param-change-registry-fwd :VoteRewardClaimedEvent]
   :param-change-registry/challenge-reward-claimed-event [:param-change-registry-fwd :ChallengeRewardClaimedEvent]
   :param-change-registry/param-change-applied-event [:param-change-registry-fwd :ParamChangeAppliedEvent]

   ;; :dank-faucet/dank-transfer-event [:dank-faucet :DankTransferEvent]
   ;; :dank-faucet/oraclize-request-event [:dank-faucet :OraclizeRequestEvent]
   ;; :dank-faucet/dank-reset-event [:dank-faucet :DankResetEvent]

   })
