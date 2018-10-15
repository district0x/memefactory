pragma solidity ^0.4.24;

import "Registry.sol";
/* import "proxy/Forwarder.sol"; */
/* import "db/EternalDb.sol"; */
import "token/minime/MiniMeToken.sol";
import "math/SafeMath.sol";

import "registryentry/RegistryEntryLib.sol";

/**
 * @title Contract created with each submission to a TCR
 *
 * @dev It contains all state and logic related to TCR challenging and voting
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single instance of it.
 * This contract is meant to be extended by domain specific registry entry contracts (Meme, ParamChange)
 */

contract RegistryEntry is ApproveAndCallFallBack {
  using SafeMath for uint;
  using RegistryEntryLib for RegistryEntryLib.Challenge;

  uint private constant oneHundred = 100;
  Registry internal constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  MiniMeToken internal constant registryToken = MiniMeToken(0xDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaD);

  address internal creator;
  uint internal version;
  uint internal deposit;
  /* uint public challengePeriodEnd; */
  RegistryEntryLib.Challenge internal challenge;

  /**
   * @dev Modifier that disables function if registry is in emergency state
   */
  modifier notEmergency() {
    require(!registry.isEmergency(), "RegistryEntry: Can't execute in emergency mode");
    _;
  }

  /**
   * @dev Modifier that disables function if registry is in emergency state
   */
  modifier onlyWhitelisted() {
    require(challenge.isWhitelisted(), "RegistryEntry: Not whitelisted");
    _;
  }

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders into single instance of this contract,
   * therefore constructor must be called explicitly.
   * Must NOT be callable multiple times
   * Transfers TCR entry token deposit from sender into this contract

   * @param _creator Creator of a meme
   * @param _version Version of Meme contract
   */
  function construct(
                     address _creator,
                     uint _version
                     )
    public
  {
    require(challenge.challengePeriodEnd == 0, "RegistryEntry: Challenge period end is not 0");
    deposit = registry.db().getUIntValue(registry.depositKey());

    require(registryToken.transferFrom(msg.sender, this, deposit), "RegistryEntry: Couldn't transfer deposit");
    /* challengePeriodEnd = now.add(registry.db().getUIntValue(registry.challengePeriodDurationKey())); */
    challenge.challengePeriodEnd = now.add(registry.db().getUIntValue(registry.challengePeriodDurationKey()));

    creator = _creator;
    version = _version;

    registry.fireRegistryEntryEvent("constructed", version);
  }

  /**
   * @dev Creates a challenge for this TCR entry
   * Must be within challenge period
   * Entry can be challenged only once
   * Transfers token deposit from challenger into this contract
   * Forks registry token (DankToken) in order to create single purpose voting token to vote about this challenge

   * @param _challenger Address of a challenger
   * @param _challengeMetaHash IPFS hash of meta data related to this challenge
   */
  function createChallenge(
                           address _challenger,
                           bytes _challengeMetaHash
                           )
    external
    notEmergency
  {
    require(challenge.isChallengePeriodActive(),"RegistryEntry: Not in challenge period");
    require(!challenge.wasChallenged(), "RegistryEntry: Was already challenged");
    require(registryToken.transferFrom(_challenger, this, deposit),"RegistryEntry: Couldn't transfer deposit");

    challenge.challenger = _challenger;
    challenge.voteQuorum = registry.db().getUIntValue(registry.voteQuorumKey());
    uint commitDuration = registry.db().getUIntValue(registry.commitPeriodDurationKey());
    uint revealDuration = registry.db().getUIntValue(registry.revealPeriodDurationKey());

    challenge.commitPeriodEnd = now.add(commitDuration);
    challenge.revealPeriodEnd = challenge.commitPeriodEnd.add(revealDuration);
    challenge.rewardPool = oneHundred.sub(registry.db().getUIntValue(registry.challengeDispensationKey())).mul(deposit).div(oneHundred);
    challenge.metaHash = _challengeMetaHash;

    registry.fireRegistryEntryEvent("challengeCreated", version);
  }

  /**
   * @dev Commits encrypted vote to challenged entry
   * Locks voter's tokens in this contract. Returns when vote is revealed
   * Must be within commit period
   * Same address can't make a second vote for the same challenge

   * @param _voter Address of a voter
   * @param _amount Amount of tokens to vote with
   * @param _secretHash Encrypted vote option with salt. sha3(voteOption, salt)
   */
  function commitVote(
                      address _voter,
                      uint _amount,
                      bytes32 _secretHash
                      )
    external
    notEmergency
  {
    require(challenge.isVoteCommitPeriodActive(), "RegistryEntry: Not in voting period");
    require(_amount > 0, "RegistryEntry: Voting amount should be more than 0");
    require(!challenge.hasVoted(_voter), "RegistryEntry: voting address has already commited a vote");
    require(registryToken.transferFrom(_voter, this, _amount), "RegistryEntry: Couldn't transfer vote amount");

    challenge.vote[_voter].secretHash = _secretHash;
    challenge.vote[_voter].amount += _amount;

    var eventData = new uint[](1);
    eventData[0] = uint(_voter);
    registry.fireRegistryEntryEvent("voteCommitted", version, eventData);
  }

  /**
   * @dev Reveals previously committed vote
   * Returns registryToken back to the voter
   * Must be within reveal period

   * @param _voter address that made the vote
   * @param _voteOption Vote option voter previously voted with
   * @param _salt Salt with which user previously encrypted his vote option
   */
  function revealVote(
                      address _voter,
                      RegistryEntryLib.VoteOption _voteOption,
                      string _salt
                      )
    external
    notEmergency
  {
    require(challenge.isVoteRevealPeriodActive(), "RegistryEntry: Reveal period is not active");
    /* require(sha3(uint(_voteOption), _salt) == challenge.vote[_voter].secretHash, "RegistryEntry: Invalid sha"); */
    require(keccak256(abi.encodePacked(uint(_voteOption), _salt)) == challenge.vote[_voter].secretHash, "RegistryEntry: Invalid sha");
    require(!challenge.isVoteRevealed(_voter), "RegistryEntry: Vote was already revealed");

    challenge.vote[_voter].revealedOn = now;
    uint amount = challenge.vote[_voter].amount;
    require(registryToken.transfer(_voter, amount), "RegistryEntry: Couldn't transfer amount");
    challenge.vote[_voter].option = _voteOption;

    if (_voteOption == RegistryEntryLib.VoteOption.VoteFor) {
      challenge.votesFor = challenge.votesFor.add(amount);
    } else if (_voteOption == RegistryEntryLib.VoteOption.VoteAgainst) {
      challenge.votesAgainst = challenge.votesAgainst.add(amount);
    } else {
      revert();
    }

    var eventData = new uint[](1);
    eventData[0] = uint(_voter);
    registry.fireRegistryEntryEvent("voteRevealed", version, eventData);
  }

  /**
   * @dev Refunds vote deposit after reveal period
   * Can be called by anybody, to claim voter's reward to him
   * Can't be called if vote was revealed
   * Can't be called twice for the same vote

   * @param _voter Address of a voter
   */
  function reclaimVoteAmount(address _voter)
    public
    notEmergency {

    if (_voter == 0x0) {
      _voter = msg.sender;
    }

    require(challenge.isVoteRevealPeriodOver(), "RegistryEntry: voting period is not yet over");
    require(!challenge.isVoteRevealed(_voter), "RegistryEntry: vote was revealed");
    require(!challenge.isVoteAmountReclaimed(_voter), "RegistryEntry: vote deposit was already reclaimed");

    uint amount = challenge.vote[_voter].amount;
    require(registryToken.transfer(_voter, amount), "RegistryEntry: token transfer failed");

    challenge.vote[_voter].reclaimedVoteAmountOn = now;

    var eventData = new uint[](1);
    eventData[0] = uint(msg.sender);

    registry.fireRegistryEntryEvent("voteAmountReclaimed", version, eventData);
  }

  /**
   * @dev Claims vote reward after reveal period
   * Voter has reward only if voted for winning option
   * Voter has reward only when revealed the vote
   * Can be called by anybody, to claim voter's reward to him

   * @param _voter Address of a voter
   */
  function claimVoteReward(
                           address _voter
                           )
    public
    notEmergency
  {
    if (_voter == 0x0) {
      _voter = msg.sender;
    }
    require(challenge.isVoteRevealPeriodOver(), "RegistryEntry: Vote reveal period is not over yet");
    require(!challenge.isVoteRewardClaimed(_voter) , "RegistryEntry: Vote rewards has been already claimed");
    require(challenge.isVoteRevealed(_voter), "RegistryEntry: Vote is not revealed yet");
    require(challenge.votedWinningVoteOption(_voter), "RegistryEntry: Can't give you a reward, your vote is not the winning option");
    uint reward = challenge.voteReward(_voter);
    require(reward > 0, "RegistryEntry: Reward should be positive");
    require(registryToken.transfer(_voter, reward), "RegistryEntry: Can't transfer reward");
    challenge.vote[_voter].claimedRewardOn = now;

    var eventData = new uint[](2);
    eventData[0] = uint(_voter);
    eventData[1] = uint(reward);
    registry.fireRegistryEntryEvent("voteRewardClaimed", version, eventData);
  }

  /**
   * @dev Claims challenger's reward after reveal period
   * Challenger has reward only if winning option is VoteAgainst
   * Can be called by anybody, to claim challenger's reward to him/her
   */
  function claimChallengeReward()
    public
    notEmergency
  {
    require(challenge.isVoteRevealPeriodOver(), "RegistryEntry: Vote reveal period is not over yet");
    require(!challenge.isChallengeRewardClaimed(),"RegistryEntry: Vote reward already claimed");
    require(!challenge.isWinningOptionVoteFor(), "RegistryEntry: Is not the winning option");
    require(registryToken.transfer(challenge.challenger, challenge.challengeReward(deposit)), "RegistryEntry: Can't transfer reward");
    challenge.claimedRewardOn = now;

    registry.fireRegistryEntryEvent("challengeRewardClaimed", version);
  }

  /**
   * @dev Simple wrapper to claim challenge and voter reward for a user
   */
  function claimAllRewards(address _user)
    external
    notEmergency
  {
    claimChallengeReward();
    claimVoteReward(_user);
  }

  /**
   * @dev Function called by MiniMeToken when somebody calls approveAndCall on it.
   * This way token can be transferred to a recipient in a single transaction together with execution
   * of additional logic

   * @param _from Sender of transaction approval
   * @param _amount Amount of approved tokens to transfer
   * @param _token Token that received the approval
   * @param _data Bytecode of a function and passed parameters, that should be called after token approval
   */
  function receiveApproval(
                           address _from,
                           uint256 _amount,
                           address _token,
                           bytes _data)
    public
  {
    require(address(this).call(_data), "RegistryEntry: couldn't call data");
  }

  /**
   * @dev Returns all basic state related to this contract for simpler offchain access
   * For challenge info see loadRegistryEntryChallenge()
   */
  function loadRegistryEntry()
    external
    constant
    returns (uint, RegistryEntryLib.Status, address, uint, uint) {
    return (
            version,
            challenge.status(),
            creator,
            deposit,
            challenge.challengePeriodEnd
            );
  }

  /**
   * @dev Returns all challenge state related to this contract for simpler offchain access
   */
  function loadRegistryEntryChallenge()
    external
    constant
    returns (uint, address, uint, bytes, uint, uint, uint, uint, uint, uint) {
    return (
            challenge.challengePeriodEnd,
            challenge.challenger,
            challenge.rewardPool,
            challenge.metaHash,
            challenge.commitPeriodEnd,
            challenge.revealPeriodEnd,
            challenge.votesFor,
            challenge.votesAgainst,
            challenge.claimedRewardOn,
            challenge.voteQuorum
            );
  }

  /**
   * @dev Returns all state related to vote for simpler offchain access
   *
   * @param _voter Address of a voter
   */
  function loadVote(address _voter)
    external
    constant
    returns (bytes32, RegistryEntryLib.VoteOption, uint, uint, uint, uint) {
    RegistryEntryLib.Vote storage vtr = challenge.vote[_voter];
    return (
            vtr.secretHash,
            vtr.option,
            vtr.amount,
            vtr.revealedOn,
            vtr.claimedRewardOn,
            vtr.reclaimedVoteAmountOn
            );
  }

}
