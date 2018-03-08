(ns memefactory.shared.graphql-schema)

(def graphql-schema "
  scalar Date

  type Query {
    meme(regEntry_address: ID!): Meme
    searchMemes: [Meme]

    paramChange(regEntry_address: ID!): ParamChange
    searchParamChanges: [ParamChange]

    user(user_address: ID!): User
    searchUsers: [User]

    searchTags: [Tag]

    parameters(db: String!, keys: [String!]): [Parameter]
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
    challenge_vote(voter: ID!): Vote
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
    challenge_vote(voter: ID!): Vote
    challenge_availableVoteAmount(voter: ID!): Int

    meme_title: String
    meme_number: Int
    meme_token: String
    meme_totalSupply: Int
    meme_imageHash: String
    meme_offeringStartPrice: Int
    meme_offeringDuration: Int
    meme_offeringSupply: Int
    meme_offeringEarnings: Int
    meme_offeringRank: Int
    meme_tags: [Tag]
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
    challenge_vote(voter: ID!): Vote
    challenge_availableVoteAmount(voter: ID!): Int

    paramChange_db: String
    paramChange_key: String
    paramChange_value: Int
    paramChange_originalValue: Int
    paramChange_appliedOn: Date
  }

  type MemePurchase {
    memePurchase_regEntry: RegEntry
    memePurchase_buyer: User
    memePurchase_amount: Int
    memePurchase_price: Int
    memePurchase_boughtOn: Date
  }

  type User {
    user_address: ID

    user_createdMemes: Int
    user_createdMemesWhitelisted: Int
    user_creatorEarnings: Int
    user_creatorRank: Int

    user_largestMemeOffering: Meme
    user_largestMemeSale: MemePurchase

    user_collectedMemes: Int
    user_collectedMemesUnique: Int
    user_largestMemePurchase: MemePurchase

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

  type Parameter {
    param_key: String
    param_value: Int
    param_db: String
  }


")
