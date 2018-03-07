(ns memefactory.shared.graphql-schema)

(def graphql-schema "
  scalar Date

  type Query {
    meme(address: ID!): Meme
    searchMemes: [Meme]

    paramChange(address: ID!): ParamChange
    searchParamChanges: [ParamChange]

    user(address: ID!): User
    searchUsers: [User]

    searchTags: [Tag]

    parameters(db: String!, keys: [String!]): [Parameter]
  }

  enum RegEntryStatus {
    challengePeriod
    commitPeriod
    revealPeriod
    blacklisted
    whitelisted
  }

  interface RegEntry {
    address: ID
    version: Int
    status: RegEntryStatus
    creator: User
    deposit: Int
    createdOn: Date
    challengePeriodEnd: Date
    challenger: User
    challengeComment: String
    votingToken: String
    rewardPool: Int
    commitPeriodEnd: Date
    revealPeriodEnd: Date
    votesFor: Int
    votesAgainst: Int
    votesTotal: Int
    claimedRewardOn: Date
    vote(voter: ID!): Vote
    availableVoteAmount(voter: ID!): Int
  }

  enum VoteOption {
    NoVote
    VoteFor
    VoteAgainst
  }

  type Vote {
    secretHash: String
    option: VoteOption
    amount: Int
    revealedOn: Date
    claimedRewardOn: Date
    reward: Int
  }

  type Tag {
    id: ID
    name: String
  }

  type Meme implements RegEntry {
    address: ID
    version: Int
    status: RegEntryStatus
    creator: User
    deposit: Int
    createdOn: Date
    challengePeriodEnd: Date
    challenger: User
    challengeComment: String
    votingToken: String
    rewardPool: Int
    commitPeriodEnd: Date
    revealPeriodEnd: Date
    votesFor: Int
    votesAgainst: Int
    votesTotal: Int
    claimedRewardOn: Date
    vote(voter: ID!): Vote
    availableVoteAmount(voter: ID!): Int

    title: String
    number: Int
    token: String
    totalSupply: Int
    imageHash: String
    offeringStartPrice: Int
    offeringDuration: Int
    offeringSupply: Int
    offeringEarnings: Int
    offeringRank: Int
    tags: [Tag]
  }

  type ParamChange implements RegEntry {
    address: ID
    version: Int
    status: RegEntryStatus
    creator: User
    deposit: Int
    createdOn: Date
    challengePeriodEnd: Date
    challenger: User
    challengeComment: String
    votingToken: String
    rewardPool: Int
    commitPeriodEnd: Date
    revealPeriodEnd: Date
    votesFor: Int
    votesAgainst: Int
    votesTotal: Int
    claimedRewardOn: Date
    vote(voter: ID!): Vote
    availableVoteAmount(voter: ID!): Int

    db: String
    key: String
    value: Int
    originalValue: Int
    appliedOn: Date
  }

  type MemePurchase {
    address: RegEntry
    buyer: User
    amount: Int
    price: Int
    boughtOn: Date
  }

  type User {
    address: ID

    createdMemes: Int
    createdMemesWhitelisted: Int
    creatorEarnings: Int
    creatorRank: Int

    largestMemeOffering: Meme
    largestMemeSale: MemePurchase

    collectedMemes: Int
    collectedMemesUnique: Int
    largestMemePurchase: MemePurchase

    createdChallenges: Int
    createdChallengesSuccess: Int
    challengerRank: Int

    participatedVotes: Int
    participatedVotesSuccess: Int
    voterEarnings: Int
    voterRank: Int

    curatorEarnings: Int
    curatorRank: Int
  }

  type Parameter {
    key: String
    value: Int
    db: String
  }


")
