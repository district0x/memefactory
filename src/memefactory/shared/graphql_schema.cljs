(ns memefactory.shared.graphql-schema)

(def graphql-schema "
  scalar Date
  scalar Keyword

  type Query {
    meme(regEntry_address: ID!): Meme
    searchMemes: MemeList

    paramChange(regEntry_address: ID!): ParamChange
    searchParamChanges: ParamChangeList

    user(user_address: ID!): User
    searchUsers: UserList

    memeAuction(memeAuction_address: ID!): MemeAuction
    searchMemeAuctions: MemeAuctionList

    searchMemeTokens: MemeTokenList

    searchTags: TagList

    param(db: String!, key: String!): Parameter
    params(db: String!, keys: [String!]): [Parameter]
  }

  enum RegEntryStatus {
    regEntry_status_challengePeriod
    regEntry_status_commitPeriod
    regEntry_status_revealPeriod
    regEntry_status_blacklisted
    regEntry_status_whitelisted
  }

  interface RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: User
    regEntry_deposit: Int
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: User
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Int
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Int
    challenge_votesAgainst: Int
    challenge_votesTotal: Int
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote
    challenge_availableVoteAmount(voter: ID!): Int
  }

  enum VoteOption {
    voteOption_noVote
    voteOption_voteFor
    voteOption_voteAgainst
  }

  type Vote {
    vote_secretHash: String
    vote_option: VoteOption
    vote_amount: Int
    vote_revealedOn: Date
    vote_claimedRewardOn: Date
    vote_reward: Int
  }

  type Tag {
    tag_id: ID
    tag_name: String
  }

  type MemeList {
    items: [Meme]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  type Meme implements RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: User
    regEntry_deposit: Int
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: User
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Int
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Int
    challenge_votesAgainst: Int
    challenge_votesTotal: Int
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote
    challenge_availableVoteAmount(voter: ID!): Int

    meme_title: String
    meme_number: Int
    meme_metaHash: String
    meme_imageHash: String
    meme_totalSupply: Int
    meme_totalMinted: Int
    meme_tokenIdStart: Int

    meme_totalTrades: Int
    meme_tradesRank: Int

    meme_tags: [Tag]
  }

  type MemeToken {
    memeToken_tokenId: ID
    memeToken_number: Int
    memeToken_owner: User
    memeToken_meme: Meme
  }

  type MemeTokenList  {
    items: [MemeToken]
    totalCount: Int
    endCursor: ID
    hasNextPage: Boolean
  }

  type TagList  {
    items: [Tag]
    totalCount: Int
    endCursor: ID
    hasNextPage: Boolean
  }

  type ParamChange implements RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: User
    regEntry_deposit: Int
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: User
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Int
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Int
    challenge_votesAgainst: Int
    challenge_votesTotal: Int
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote
    challenge_availableVoteAmount(voter: ID!): Int

    paramChange_db: String
    paramChange_key: String
    paramChange_value: Int
    paramChange_originalValue: Int
    paramChange_appliedOn: Date
  }
  
  type ParamChangeList {
    items: [ParamChange]
    totalCount: Int
    endCursor: ID
    hasNextPage: Boolean
  }  

  type MemeAuctionList {
    items: [MemeAuction]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  type MemeAuction {
    memeAuction_address: ID
    memeAuction_seller: User
    memeAuction_buyer: User
    memeAuction_startPrice: Int
    memeAuction_endPrice: Int
    memeAuction_duration: Int
    memeAuction_startedOn: Date
    memeAuction_boughtOn: Date
    memeAuction_memeToken: MemeToken
  }

  type User {
    user_address: ID
    user_createdMemes: Int
    user_createdMemesWhitelisted: Int
    user_creatorEarnings: Int
    user_creatorRank: Int

    user_largestMemeAuctionSale: MemeAuction

    user_collectedTokenIds: Int
    user_collectedMemes: Int
    user_largestMemeAuctionBuy: MemeAuction

    user_createdChallenges: Int
    user_createdChallengesSuccess: Int
    user_challengerRank: Int

    user_participatedVotes: Int
    user_participatedVotesSuccess: Int
    user_voterEarnings: Int
    user_voterRank: Int

    user_curatorEarnings: Int
    user_curatorRank: Int
  }
  
  type UserList {
    items: [User]
    totalCount: Int
    endCursor: ID
    hasNextPage: Boolean
  }

  type Parameter {
    param_db: ID
    param_key: ID
    param_value: Int
  }

")
