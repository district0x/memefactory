pragma solidity ^0.4.11;

import "./token/ERC20.sol";
import "./Parameterizer.sol";
import "./PLCRVoting.sol";
import "./MemeFactory.sol";
import "./ownership/Ownable.sol";

contract Registry is Ownable {

  // ------
  // EVENTS
  // ------

  event OnApplication(address listingHash, uint deposit);
  event OnChallenge(address listingHash, uint deposit, uint pollID, string data);
  event OnNewListingWhitelisted(address listingHash);
  event OnChallengeFailed(uint challengeID);
  event OnChallengeSucceeded(uint challengeID);
  event OnRewardClaimed(address voter, uint challengeID, uint reward);

  struct Listing {
    uint applicationExpiry; // Expiration date of apply stage
    bool whitelisted;       // Indicates registry status
    address owner;          // Owner of Listing
    uint deposit;           // Number of tokens deposited when applying
    uint challengeID;       // Corresponds to a PollID in PLCRVoting
    uint64 whitelistedOn;   // Date of whitelisting
  }

  struct Challenge {
    uint rewardPool;        // (remaining) Pool of tokens to be distributed to winning voters
    address challenger;     // Owner of Challenge
    bool resolved;          // Indication of if challenge is resolved
    uint stake;             // Number of tokens at stake for either party during challenge
    uint totalTokens;       // (remaining) Number of tokens used in voting by the winning side
    mapping(address => bool) voterClaimedReward; // Indicates whether a voter has claimed a reward yet
  }

  // Maps challengeIDs to associated challenge data
  mapping(uint => Challenge) public challenges;

  // Maps listingHashes to associated listingHash data
  mapping(address => Listing) public listings;

  // Global Variables
  ERC20 public token;
  PLCRVoting public voting;
  Parameterizer public parameterizer;
  MemeFactory public memeFactory;
  string public version = '1';

  // ------------
  // CONSTRUCTOR:
  // ------------

  /**
  @dev Contructor         Sets the addresses for token, voting, and parameterizer
  @param _tokenAddr       Address of the TCR's intrinsic ERC20 token
  @param _plcrAddr        Address of a PLCR voting contract for the provided token
  @param _paramsAddr      Address of a Parameterizer contract
  */
  function Registry(
    address _tokenAddr,
    address _plcrAddr,
    address _paramsAddr,
    address _memeFactory
  )
    Ownable()
    public
  {
    token = ERC20(_tokenAddr);
    voting = PLCRVoting(_plcrAddr);
    parameterizer = Parameterizer(_paramsAddr);
    memeFactory = MemeFactory(_memeFactory);
  }

  modifier onlyMemeFactory() {
    require(msg.sender == address(memeFactory));
    _;
  }

  function setMemeFactory(MemeFactory _memeFactory) onlyOwner {
    memeFactory = _memeFactory;
  }

  // --------------------
  // PUBLISHER INTERFACE:
  // --------------------

  /**
  @dev                Allows a user to start an application. Takes tokens from user and sets
                      apply stage end time.
  @param _listingHash The hash of a potential listing a user is applying to add to the registry
  */
  function apply(address _listingHash) onlyMemeFactory public {
    require(!isWhitelisted(_listingHash));
    require(!appWasMade(_listingHash));
    var deposit = parameterizer.get("deposit");

    // Sets owner
    Listing storage listing = listings[_listingHash];
    listing.owner = msg.sender;

    // Transfers tokens from user to Registry contract
    require(token.transferFrom(listing.owner, this, deposit));

    // Sets apply stage end time
    listing.applicationExpiry = block.timestamp + parameterizer.get("applyStageLen");
    listing.deposit = deposit;

    OnApplication(_listingHash, deposit);
  }

  // -----------------------
  // TOKEN HOLDER INTERFACE:
  // -----------------------

  /**
  @dev                Starts a poll for a listingHash which is either in the apply stage or
                      already in the whitelist. Tokens are taken from the challenger and the
                      applicant's deposits are locked.
  @param _listingHash The listingHash being challenged, whether listed or in application
  @param _data        Extra data relevant to the challenge. Think IPFS hashes.
  */
  function challenge(address _listingHash, string _data) external returns (uint challengeID) {
    Listing storage listing = listings[_listingHash];
    var deposit = listing.deposit;

    // Listing must be in apply stage and not whitelisted
    require(appWasMade(_listingHash) && !listing.whitelisted);
    // Prevent multiple challenges
    require(listing.challengeID == 0 || challenges[listing.challengeID].resolved);

    // Takes tokens from challenger
    require(token.transferFrom(msg.sender, this, deposit));

    // Starts poll
    uint pollID = voting.startPoll(
      parameterizer.get("voteQuorum"),
      parameterizer.get("commitStageLen"),
      parameterizer.get("revealStageLen")
    );

    challenges[pollID] = Challenge({
      challenger : msg.sender,
      rewardPool : ((100 - parameterizer.get("dispensationPct")) * deposit) / 100,
      stake : deposit,
      resolved : false,
      totalTokens : 0
      });

    // Updates listingHash to store most recent challenge
    listing.challengeID = pollID;

    OnChallenge(_listingHash, deposit, pollID, _data);
    return pollID;
  }

  /**
  @dev                Updates a listingHash's status from 'application' to 'listing' or resolves
                      a challenge if one exists.
  @param _listingHash The listingHash whose status is being updated
  */
  function updateStatus(address _listingHash) public {
    if (canBeWhitelisted(_listingHash)) {
      whitelistApplication(_listingHash);
      OnNewListingWhitelisted(_listingHash);
    } else if (challengeCanBeResolved(_listingHash)) {
      resolveChallenge(_listingHash);
    } else {
      revert();
    }
  }

  // ----------------
  // TOKEN FUNCTIONS:
  // ----------------

  /**
  @dev                Called by a voter to claim their reward for each completed vote. Someone
                      must call updateStatus() before this can be called.
  @param _challengeID The PLCR pollID of the challenge a reward is being claimed for
  @param _salt        The salt of a voter's commit hash in the given poll
  */
  function claimVoterReward(uint _challengeID, uint _salt) public {
    // Ensures the voter has not already claimed tokens and challenge results have been processed
    require(challenges[_challengeID].voterClaimedReward[msg.sender] == false);
    require(challenges[_challengeID].resolved == true);

    uint voterTokens = voting.getNumPassingTokens(msg.sender, _challengeID, _salt);
    uint reward = voterReward(msg.sender, _challengeID, _salt);

    // Subtracts the voter's information to preserve the participation ratios
    // of other voters compared to the remaining pool of rewards
    challenges[_challengeID].totalTokens -= voterTokens;
    challenges[_challengeID].rewardPool -= reward;

    require(token.transfer(msg.sender, reward));

    // Ensures a voter cannot claim tokens again
    challenges[_challengeID].voterClaimedReward[msg.sender] = true;

    OnRewardClaimed(msg.sender, _challengeID, reward);
  }

  // --------
  // GETTERS:
  // --------

  /**
  @dev                Calculates the provided voter's token reward for the given poll.
  @param _voter       The address of the voter whose reward balance is to be returned
  @param _challengeID The pollID of the challenge a reward balance is being queried for
  @param _salt        The salt of the voter's commit hash in the given poll
  @return             The uint indicating the voter's reward
  */
  function voterReward(address _voter, uint _challengeID, uint _salt)
  public view returns (uint) {
    uint totalTokens = challenges[_challengeID].totalTokens;
    uint rewardPool = challenges[_challengeID].rewardPool;
    uint voterTokens = voting.getNumPassingTokens(_voter, _challengeID, _salt);
    return (voterTokens * rewardPool) / totalTokens;
  }

  /**
  @dev                Determines whether the given listingHash be whitelisted.
  @param _listingHash The listingHash whose status is to be examined
  */
  function canBeWhitelisted(address _listingHash) view public returns (bool) {
    uint challengeID = listings[_listingHash].challengeID;

    // Ensures that the application was made,
    // the application period has ended,
    // the listingHash can be whitelisted,
    // and either: the challengeID == 0, or the challenge has been resolved.
    if (
      appWasMade(_listingHash) &&
      listings[_listingHash].applicationExpiry < now &&
    !isWhitelisted(_listingHash) &&
    (challengeID == 0 || challenges[challengeID].resolved == true)
    ) {return true;}

    return false;
  }

  /**
  @dev                Returns true if the provided listingHash is whitelisted
  @param _listingHash The listingHash whose status is to be examined
  */
  function isWhitelisted(address _listingHash) view public returns (bool whitelisted) {
    return listings[_listingHash].whitelisted;
  }

  /**
  @dev                Returns true if apply was called for this listingHash
  @param _listingHash The listingHash whose status is to be examined
  */
  function appWasMade(address _listingHash) view public returns (bool exists) {
    return listings[_listingHash].applicationExpiry > 0;
  }

  /**
  @dev                Returns true if the application/listingHash has an unresolved challenge
  @param _listingHash The listingHash whose status is to be examined
  */
  function challengeExists(address _listingHash) view public returns (bool) {
    uint challengeID = listings[_listingHash].challengeID;

    return (listings[_listingHash].challengeID > 0 && !challenges[challengeID].resolved);
  }

  /**
  @dev                Determines whether voting has concluded in a challenge for a given
                      listingHash. Throws if no challenge exists.
  @param _listingHash A listingHash with an unresolved challenge
  */
  function challengeCanBeResolved(address _listingHash) view public returns (bool) {
    uint challengeID = listings[_listingHash].challengeID;

    require(challengeExists(_listingHash));

    return voting.pollEnded(challengeID);
  }

  /**
  @dev                Determines the number of tokens awarded to the winning party in a challenge.
  @param _challengeID The challengeID to determine a reward for
  */
  function challengeWinnerReward(uint _challengeID) public view returns (uint) {
    require(!challenges[_challengeID].resolved && voting.pollEnded(_challengeID));

    // Edge case, nobody voted, give all tokens to the challenger.
    if (voting.getTotalNumberOfTokensForWinningOption(_challengeID) == 0) {
      return 2 * challenges[_challengeID].stake;
    }

    return (2 * challenges[_challengeID].stake) - challenges[_challengeID].rewardPool;
  }

  /**
  @dev                Getter for Challenge voterClaimedReward mappings
  @param _challengeID The challengeID to query
  @param _voter       The voter whose claim status to query for the provided challengeID
  */
  function voterClaimedReward(uint _challengeID, address _voter) public view returns (bool) {
    return challenges[_challengeID].voterClaimedReward[_voter];
  }

  // ----------------
  // PRIVATE FUNCTIONS:
  // ----------------

  /**
  @dev                Determines the winner in a challenge. Rewards the winner tokens and
                      either whitelists or de-whitelists the listingHash.
  @param _listingHash A listingHash with a challenge that is to be resolved
  */
  function resolveChallenge(address _listingHash) private {
    uint challengeID = listings[_listingHash].challengeID;

    // Calculates the winner's reward,
    // which is: (winner's full stake) + (dispensationPct * loser's stake)
    uint reward = challengeWinnerReward(challengeID);

    // Records whether the listingHash is a listingHash or an application
    bool wasWhitelisted = isWhitelisted(_listingHash);

    // Case: challenge failed
    if (voting.isPassed(challengeID)) {
      whitelistApplication(_listingHash);
      // TODO reward size?
      require(token.transfer(listings[_listingHash].owner, reward / 2));

      OnChallengeFailed(challengeID);
      if (!wasWhitelisted) {OnNewListingWhitelisted(_listingHash);}
    }
    // Case: challenge succeeded
    else {
      // Transfer the reward to the challenger
      require(token.transfer(challenges[challengeID].challenger, reward));

      OnChallengeSucceeded(challengeID);
    }

    // Sets flag on challenge being processed
    challenges[challengeID].resolved = true;

    // Stores the total tokens used for voting by the winning side for reward purposes
    challenges[challengeID].totalTokens =
    voting.getTotalNumberOfTokensForWinningOption(challengeID);
  }

  /**
  @dev                Called by updateStatus() if the applicationExpiry date passed without a
                      challenge being made. Called by resolveChallenge() if an
                      application/listing beat a challenge.
  @param _listingHash The listingHash of an application/listingHash to be whitelisted
  */
  function whitelistApplication(address _listingHash) private {
    listings[_listingHash].whitelisted = true;
    listings[_listingHash].whitelistedOn = uint64(now);
  }
}
