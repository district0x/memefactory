(ns memefactory.shared.graphql-schema)

(def graphql-schema "
  scalar Date
  scalar Keyword

  type Query {
    meme(regEntry_address: ID!): Meme
    searchMemes(statuses: [RegEntryStatus],
                title: String,
                tags: [ID],
                tagsOr: [ID],
                orderBy: MemesOrderBy,
                orderDir: OrderDir,
                groupBy: MemesGroupBy,
                owner: String,
                creator: String,
                curator: String,
                challenger: String,
                voter: String,
                challenged: Boolean,
                first: Int,
                after: String
    ): MemeList
    searchMemeTokens(statuses: [RegEntryStatus],
                     orderBy: MemeTokensOrderBy,
                     orderDir: OrderDir,
                     owner: String,
                     first: Int,
                     after: String
    ): MemeTokenList

    memeAuction(memeAuction_address: ID!): MemeAuction
    searchMemeAuctions(
      title: String,
      tags: [ID],
      tagsOr: [ID],
      orderBy: MemeAuctionsOrderBy,
      orderDir: OrderDir,
      groupBy: MemeAuctionsGroupBy,
      statuses: [MemeAuctionStatus],
      seller: String,
      first: Int,
      after: String
    ): MemeAuctionList

    searchTags(first: Int, after: String): TagList

    paramChange(regEntry_address: ID!): ParamChange
    searchParamChanges(key: String!,
                       db: String!,
                       orderBy: ParamChangesOrderBy,
                       orderDir: OrderDir,
                       groupBy: ParamChangesGroupBy,
                       first: Int,
                       after: String
    ): ParamChangeList

    user(user_address: ID!): User
    searchUsers(orderBy: UsersOrderBy,
                orderDir: OrderDir,
                first: Int,
                after: String
    ): UserList

    param(db: String!, key: String!): Parameter
    params(db: String!, keys: [String!]): [Parameter]
    overallStats : OverallStats
    config: Config
  }

  type PhoneVerificationResponse {
    id: String
    status: Int
    success: Boolean
    msg: String!
  }

  type EncryptedVerificationPayload {
    success: Boolean
    payload: String
  }

  type Mutation {
    sendVerificationCode(countryCode: String!,
                         phoneNumber: String!
    ): PhoneVerificationResponse

    encryptVerificationPayload(countryCode: String,
                               phoneNumber: String,
                               verificationCode: String
    ): EncryptedVerificationPayload

    blacklistRegEntry(address: ID!, token: String!): Boolean
  }

  enum OrderDir {
    asc
    desc
  }

  enum MemesOrderBy {
    memes_orderBy_revealPeriodEnd
    memes_orderBy_commitPeriodEnd
    memes_orderBy_challengePeriodEnd
    memes_orderBy_totalTradeVolume
    memes_orderBy_dailyTotalVotes
    memes_orderBy_createdOn
    memes_orderBy_number
    memes_orderBy_totalMinted
    memes_orderBy_averagePrice
    memes_orderBy_highestSingleSale
  }

  enum MemesGroupBy {
    memes_groupBy_title
  }

  enum MemeTokensOrderBy {
    memeTokens_orderBy_memeNumber
    memeTokens_orderBy_memeTitle
    memeTokens_orderBy_transferredOn
    memeTokens_orderBy_tokenId
  }

  enum MemeAuctionsOrderBy {
    memeAuctions_orderBy_seller
    memeAuctions_orderBy_buyer
    memeAuctions_orderBy_price
    memeAuctions_orderBy_startedOn
    memeAuctions_orderBy_boughtOn
    memeAuctions_orderBy_tokenId
    memeAuctions_orderBy_memeTotalMinted
    memeAuctions_orderBy_random
  }

  enum UsersOrderBy {
    users_orderBy_address
    users_orderBy_voterTotalEarned
    users_orderBy_challengerTotalEarned
    users_orderBy_curatorTotalEarned
    users_orderBy_totalParticipatedVotesSuccess
    users_orderBy_totalParticipatedVotes
    users_orderBy_totalCreatedChallengesSuccess
    users_orderBy_totalCreatedChallenges
    users_orderBy_totalCollectedMemes
    users_orderBy_totalCollectedTokenIds
    users_orderBy_totalCreatedMemesWhitelisted
    users_orderBy_totalCreatedMemes
    users_orderBy_totalEarned
    users_orderBy_bestSingleCardSale
  }

  enum MemeAuctionsGroupBy {
    memeAuctions_groupBy_cheapest
  }

  enum ParamChangesOrderBy {
    paramChanges_orderBy_appliedOn
  }

   enum ParamChangesGroupBy {
     paramChanges_groupBy_key
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
    regEntry_deposit: Float
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: User
    challenge_createdOn: Date
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Float
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Float
    challenge_votesAgainst: Float
    challenge_votesTotal: Float
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote
    challenge_voteWinningVoteOption(vote_voter: ID!): Boolean
    challenge_allRewards(user_address: ID!): Rewards
  }

  type Rewards{
    challenge_rewardAmount: Float
    vote_rewardAmount: Float
  }

  enum VoteOption {
    voteOption_noVote
    voteOption_voteFor
    voteOption_voteAgainst
    voteOption_notRevealed
  }

  type Vote {
    vote_secretHash: String
    vote_option: VoteOption
    vote_amount: Float
    vote_revealedOn: Date
    vote_claimedRewardOn: Date
    vote_reclaimedAmountOn: Date
    vote_reward: Float
  }

  type Meme implements RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: User
    regEntry_deposit: Float
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: User
    challenge_createdOn: Date
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Float
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Float
    challenge_votesAgainst: Float
    challenge_votesTotal: Float
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote
    challenge_voteWinningVoteOption(vote_voter: ID!): Boolean
    challenge_allRewards(user_address: ID!): Rewards

    \"Balance of voting token of a voter. This is client-side only, server doesn't return this\"
    challenge_availableVoteAmount(voter: ID!): Int

    meme_title: String
    meme_number: Int
    meme_metaHash: String
    meme_imageHash: String
    meme_totalSupply: Int
    meme_totalMinted: Int
    meme_tokenIdStart: Int

    meme_totalTradeVolume: Float
    meme_totalTradeVolumeRank: Int
    meme_averagePrice: Float
    meme_highestSingleSale: Float

    meme_ownedMemeTokens(owner: String): [MemeToken]

    meme_tags: [Tag]
    meme_memeAuctions(orderBy: MemeAuctionsOrderBy,
                      orderDir: OrderDir,
                      completed: Boolean): [MemeAuction]
  }

  type MemeList {
    items: [Meme]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  type Tag {
    tag_name: String
  }

  type TagList  {
    items: [Tag]
    totalCount: Int
    endCursor: ID
    hasNextPage: Boolean
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

  enum MemeAuctionStatus {
    memeAuction_status_active
    memeAuction_status_canceled
    memeAuction_status_done
  }

  type MemeAuction {
    memeAuction_address: ID
    memeAuction_seller: User
    memeAuction_buyer: User
    memeAuction_startPrice: Float
    memeAuction_endPrice: Float
    memeAuction_boughtFor: Float
    memeAuction_duration: Int
    memeAuction_startedOn: Date
    memeAuction_boughtOn: Date
    memeAuction_status: MemeAuctionStatus
    memeAuction_memeToken: MemeToken
    memeAuction_description: String
  }

  type MemeAuctionList {
    items: [MemeAuction]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  type ParamChange implements RegEntry {
    regEntry_address: ID
    regEntry_version: Int
    regEntry_status: RegEntryStatus
    regEntry_creator: User
    regEntry_deposit: Float
    regEntry_createdOn: Date
    regEntry_challengePeriodEnd: Date
    challenge_challenger: User
    challenge_createdOn: Date
    challenge_comment: String
    challenge_votingToken: String
    challenge_rewardPool: Float
    challenge_commitPeriodEnd: Date
    challenge_revealPeriodEnd: Date
    challenge_votesFor: Float
    challenge_votesAgainst: Float
    challenge_votesTotal: Float
    challenge_claimedRewardOn: Date
    challenge_vote(vote_voter: ID!): Vote
    challenge_voteWinningVoteOption(vote_voter: ID!): Boolean
    challenge_allRewards(user_address: ID!): Rewards

    \"Balance of voting token of a voter. This is client-side only, server doesn't return this\"
    challenge_availableVoteAmount(voter: ID!): Int

    paramChange_db: String
    paramChange_key: String
    paramChange_value: Float
    paramChange_originalValue: Float
    paramChange_appliedOn: Date
  }

  type ParamChangeList {
    items: [ParamChange]
    totalCount: Int
    endCursor: String
    hasNextPage: Boolean
  }

  type User {
    \"Ethereum address of an user\"
    user_address: ID

    \"Total number of memes submitted by user\"
    user_totalCreatedMemes: Int

    \"Total number of memes submitted by user, which successfully got into TCR\"
    user_totalCreatedMemesWhitelisted: Int

    \"Largest sale creator has done with his newly minted meme\"
    user_creatorLargestSale: MemeAuction

    \"Position of a creator in leaderboard according to user_totalCreatedMemesWhitelisted\"
    user_creatorRank: Int

    \"Position of a collector in leaderboard \"
    user_collectorRank: Int

    \"Amount of meme tokenIds owned by user\"
    user_totalCollectedTokenIds: Int

    \"Amount of unique memes owned by user\"
    user_totalCollectedMemes: Int

    \"Largest auction user sold, in terms of price\"
    user_largestSale: MemeAuction

    \"Largest auction user bought into, in terms of price\"
    user_largestBuy: MemeAuction

    \"Amount of challenges user created\"
    user_totalCreatedChallenges: Int

    \"Amount of challenges user created and ended up in his favor\"
    user_totalCreatedChallengesSuccess: Int

    \"Total amount of DANK token user received from challenger rewards\"
    user_challengerTotalEarned: Float

    \"Total amount of DANK token user received from challenger rewards\"
    user_challengerRank: Int

    \"Amount of different votes user participated in\"
    user_totalParticipatedVotes: Int

    \"Amount of different votes user voted for winning option\"
    user_totalParticipatedVotesSuccess: Int

    \"Amount of DANK token user received for voting for winning option\"
    user_voterTotalEarned: Float

    \"Position of voter in leaderboard according to user_voterTotalEarned\"
    user_voterRank: Int

    \"Sum of user_challengerTotalEarned and user_voterTotalEarned\"
    user_curatorTotalEarned: Float

    \"Users total earned as a creator \"
    user_creatorTotalEarned: Float

    \"Position of curator in leaderboard according to user_curatorTotalEarned\"
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
    param_value: Float
  }

  type OverallStats {
    totalMemesCount: Int
    totalTokensCount: Int
  }

  type Config {
   ipfs: Ipfs
   ui: UIConfig
  }

  type UIConfig{
    publicKey: String
    rootUrl: String
  }

  type Ipfs {
    host: String
    endpoint: String
    gateway: String
  }
  "
  )
