pragma solidity ^0.4.18;

import "Registry.sol";
import "forwarder/Forwarder.sol";
import "db/EternalDb.sol";
import "token/minime/MiniMeToken.sol";
import "math/SafeMath.sol";

/**
 * @title Contract created with each submission to a TCR
 *
 * @dev It contains all state and logic related to TCR challenging and voting
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 * This contract is meant to be extended by domain specific registry entry contracts (Meme, ParamChange)
 */

contract RegistryEntry is ApproveAndCallFallBack {
  using SafeMath for uint;

  Registry public constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  MiniMeToken public constant registryToken = MiniMeToken(0xDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaD);
  bytes32 public constant challengePeriodDurationKey = sha3("challengePeriodDuration");
  bytes32 public constant commitPeriodDurationKey = sha3("commitPeriodDuration");
  bytes32 public constant revealPeriodDurationKey = sha3("revealPeriodDuration");
  bytes32 public constant depositKey = sha3("deposit");
  bytes32 public constant challengeDispensationKey = sha3("challengeDispensation");
  bytes32 public constant voteQuorumKey = sha3("voteQuorum");

  enum Status {ChallengePeriod, CommitPeriod, RevealPeriod, Blacklisted, Whitelisted}

  address public creator;
  uint public version;
  uint public deposit;
  uint public challengePeriodEnd;
  Challenge public challenge;

  struct Challenge {
    address challenger;
    MiniMeToken votingToken;
    uint voteQuorum;
    uint rewardPool;
    bytes metaHash;
    uint commitPeriodEnd;
    uint revealPeriodEnd;
    uint votesFor;
    uint votesAgainst;
    mapping(address => Voter) voter;
  }

  struct Voter {
    bytes32 secretHash;
    VoteOption voteOption;
    uint amount;
    uint revealedOn;
    uint claimedRewardOn;
  }

  enum VoteOption {NoVote, VoteFor, VoteAgainst}

  /**
   * @dev Modifier that disables function if registry is in emergency state
   */
  modifier notEmergency() {
    require(!registry.isEmergency());
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
    require(challengePeriodEnd == 0);
    deposit = registry.db().getUIntValue(depositKey);

    require(registryToken.transferFrom(msg.sender, this, deposit));
    challengePeriodEnd = now.add(registry.db().getUIntValue(challengePeriodDurationKey));
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
  public
  notEmergency
  {
    require(isChallengePeriodActive());
    require(!wasChallenged());
    require(registryToken.transferFrom(_challenger, this, deposit));

    challenge.challenger = _challenger;
    challenge.votingToken = MiniMeToken(registryToken.createCloneToken("", 18, "", 0, true));
    challenge.votingToken.changeController(0x0);
    challenge.voteQuorum = registry.db().getUIntValue(voteQuorumKey);
    uint commitDuration = registry.db().getUIntValue(commitPeriodDurationKey);
    uint revealDuration = registry.db().getUIntValue(revealPeriodDurationKey);
    uint deposit = registry.db().getUIntValue(depositKey);
    challenge.commitPeriodEnd = now.add(commitDuration);
    challenge.revealPeriodEnd = challenge.commitPeriodEnd.add(revealDuration);
    challenge.rewardPool = ((100 - registry.db().getUIntValue(challengeDispensationKey)).mul(deposit)) / 100;
    challenge.metaHash = _challengeMetaHash;

    registry.fireRegistryEntryEvent("challengeCreated", version);
  }

  /**
   * @dev Commits encrypted vote to challenged entry
   * Must be within commit period
   * Voting takes full balance of voter's voting token

   * @param _voter Address of a voter
   * @param _secretHash Encrypted vote option with salt. sha3(voteOption, salt)
   */
  function commitVote(
    address _voter,
    bytes32 _secretHash
  )
  public
  notEmergency
  {
    require(isVoteCommitPeriodActive());
    uint amount = challenge.votingToken.balanceOf(_voter);
    require(amount > 0);
    require(challenge.votingToken.transferFrom(_voter, this, amount));
    challenge.voter[_voter].secretHash = _secretHash;
    challenge.voter[_voter].amount = amount;
    registry.fireRegistryEntryEvent("voteCommited", version);
  }

  /**
   * @dev Reveals previously committed vote
   * Must be within reveal period

   * @param _voteOption Vote option voter previously voted with
   * @param _salt Salt with which user previously encrypted his vote option
   */
  function revealVote(
    VoteOption _voteOption,
    string _salt
  )
  public
  notEmergency
  {
    require(isVoteRevealPeriodActive());
    require(sha3(uint(_voteOption), _salt) == challenge.voter[msg.sender].secretHash);
    require(!hasVoterRevealed(msg.sender));

    challenge.voter[msg.sender].revealedOn = now;
    uint amount = challenge.voter[msg.sender].amount;
    challenge.voter[msg.sender].voteOption = _voteOption;
    if (_voteOption == VoteOption.VoteFor) {
      challenge.votesFor = challenge.votesFor.add(amount);
    } else if (_voteOption == VoteOption.VoteAgainst) {
      challenge.votesAgainst = challenge.votesAgainst.add(amount);
    } else {
      revert();
    }

    registry.fireRegistryEntryEvent("voteRevealed", version);
  }

  /**
   * @dev Claims voter's reward after reveal period
   * Voter has reward only if voted for winning option
   * Voter has reward only when revealed the vote
   * Can be called by anybody, to claim voter's reward to him

   * @param _voter Address of a voter
   */
  function claimVoterReward(
    address _voter
  )
  public
  notEmergency
  {
    if (_voter == 0x0) {
      _voter = msg.sender;
    }
    require(isVoteRevealPeriodOver());
    require(!hasVoterClaimedReward(_voter));
    require(hasVoterRevealed(_voter));
    require(votedWinningVoteOption(_voter));
    uint reward = voterReward(_voter);
    require(registryToken.transfer(_voter, reward));
    challenge.voter[_voter].claimedRewardOn = now;
    registry.fireRegistryEntryEvent("voterRewardClaimed", version);
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
    require(this.call(_data));
  }

  /**
   * @dev Returns current status of a registry entry

   * @return Status
   */
  function status() public constant returns (Status) {
    if (isChallengePeriodActive() && !wasChallenged()) {
      return Status.ChallengePeriod;
    } else if (isVoteCommitPeriodActive()) {
      return Status.CommitPeriod;
    } else if (isVoteRevealPeriodActive()) {
      return Status.RevealPeriod;
    } else if (isVoteRevealPeriodOver()) {
      if (winningVoteIsFor()) {
        return Status.Whitelisted;
      } else {
        return Status.Blacklisted;
      }
    } else {
      return Status.Whitelisted;
    }
  }

  function isChallengePeriodActive() public constant returns (bool) {
    return now <= challengePeriodEnd;
  }

  function isWhitelisted() public constant returns (bool) {
    return status() == Status.Whitelisted;
  }

  function isBlacklisted() public constant returns (bool) {
    return status() == Status.Blacklisted;
  }

  /**
   * @dev Returns date when registry entry was whitelisted
   * Since this doesn't happen with any transaction, it's either reveal or challenge period end

   * @return UNIX time of whitelisting
   */
  function whitelistedOn() public constant returns (uint) {
    if (!isWhitelisted()) {
      return 0;
    }
    if (wasChallenged()) {
      return challenge.revealPeriodEnd;
    } else {
      return challengePeriodEnd;
    }
  }

  function wasChallenged() public constant returns (bool) {
    return challenge.challenger != 0x0;
  }

  function isVoteCommitPeriodActive() public constant returns (bool) {
    return now <= challenge.commitPeriodEnd;
  }

  function isVoteRevealPeriodActive() public constant returns (bool) {
    return !isVoteCommitPeriodActive() && now <= challenge.revealPeriodEnd;
  }

  function isVoteRevealPeriodOver() public constant returns (bool) {
    return challenge.revealPeriodEnd > 0 && now > challenge.revealPeriodEnd;
  }

  /**
   * @dev Returns all state related to voter for simpler offchain access
   *
   * @param _voter Address of a voter
   */
  function voter(address _voter) public constant returns (bytes32, VoteOption, uint, uint, uint) {
    Voter vtr = challenge.voter[_voter];
    return (
    vtr.secretHash,
    vtr.voteOption,
    vtr.amount,
    vtr.revealedOn,
    vtr.claimedRewardOn
    );
  }

  function hasVoterRevealed(address _voter) public constant returns (bool) {
    return challenge.voter[_voter].revealedOn > 0;
  }

  function hasVoterClaimedReward(address _voter) public constant returns (bool) {
    return challenge.voter[_voter].claimedRewardOn > 0;
  }

  /**
   * @dev Returns winning vote option in held voting according to vote quorum
   * If voteQuorum is 50, any majority of votes will win
   * If voteQuorum is 24, only 25 votes out of 100 is enough to VoteFor be winning option
   *
   * @return Winning vote option
   */
  function winningVoteOption() public constant returns (VoteOption) {
    if (!isVoteRevealPeriodOver()) {
      return VoteOption.NoVote;
    }

    if (challenge.votesFor.mul(100) > challenge.voteQuorum.mul(challenge.votesFor.add(challenge.votesAgainst))) {
      return VoteOption.VoteFor;
    } else {
      return VoteOption.VoteAgainst;
    }
  }

  /**
   * @dev Returns whether VoteFor is winning vote option
   *
   * @return True if VoteFor is winning option
   */
  function winningVoteIsFor() public constant returns (bool) {
    return winningVoteOption() == VoteOption.VoteFor;
  }

  /**
   * @dev Returns amount of votes for winning vote option
   *
   * @return Amount of votes
   */
  function winningVotesAmount() public constant returns (uint) {
    VoteOption voteOption = winningVoteOption();

    if (voteOption == VoteOption.VoteFor) {
      return challenge.votesFor;
    } else if (voteOption == VoteOption.VoteAgainst) {
      return challenge.votesAgainst;
    } else {
      return 0;
    }
  }

  /**
   * @dev Returns token reward amount belonging to a voter for voting for a winning option
   * @param _voter Address of a voter
   *
   * @return Amount of tokens
   */
  function voterReward(address _voter) public constant returns (uint) {
    uint winningAmount = winningVotesAmount();
    uint voterAmount = 0;
    if (votedWinningVoteOption(_voter)) {
      voterAmount = challenge.voter[_voter].amount;
    }
    return (voterAmount.mul(challenge.rewardPool)) / winningAmount;
  }

  /**
   * @dev Returns whether voter voted for winning vote option
   * @param _voter Address of a voter
   *
   * @return True if voter voted for a winning vote option
   */
  function votedWinningVoteOption(address _voter) public constant returns (bool) {
    return challenge.voter[_voter].voteOption == winningVoteOption();
  }

  /**
   * @dev Returns all state related to this contract for simpler offchain access
   */
  function loadRegistryEntry() public constant returns (uint, Status, address, uint, uint, address, address, uint, bytes, uint, uint, uint, uint) {
    return (
    version,
    status(),
    creator,
    deposit,
    challengePeriodEnd,
    challenge.challenger,
    challenge.votingToken,
    challenge.rewardPool,
    challenge.metaHash,
    challenge.commitPeriodEnd,
    challenge.revealPeriodEnd,
    challenge.votesFor,
    challenge.votesAgainst
    );
  }
}