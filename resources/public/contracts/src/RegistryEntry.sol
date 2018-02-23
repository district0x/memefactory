pragma solidity ^0.4.18;

import "Registry.sol";
import "forwarder/Forwarder.sol";
import "db/EternalDb.sol";
import "token/minime/MiniMeToken.sol";

contract RegistryEntry is ApproveAndCallFallBack {
  Registry public constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  MiniMeToken public constant registryToken = MiniMeToken(0xDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaD);
  bytes32 public constant challengePeriodDurationKey = sha3("challengePeriodDuration");
  bytes32 public constant commitPeriodDurationKey = sha3("commitPeriodDuration");
  bytes32 public constant revealPeriodDurationKey = sha3("revealPeriodDuration");
  bytes32 public constant depositKey = sha3("deposit");
  bytes32 public constant challengeDispensationKey = sha3("challengeDispensation");

  enum Status {ChallengePeriod, CommitPeriod, RevealPeriod, Blacklisted, Whitelisted}

  address public creator;
  uint public version;
  uint public deposit;
  uint public challengePeriodEnd;
  Challenge public challenge;

  struct Challenge {
    address challenger;
    MiniMeToken votingToken;
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

  modifier notEmergency() {
    require(!registry.isEmergency());
    _;
  }

  modifier onlyConstructed() {
    require(challengePeriodEnd > 0);
    _;
  }

  function construct(
    address _creator,
    uint _version
  )
  public
  {
    require(challengePeriodEnd == 0);
    deposit = registry.db().getUIntValue(depositKey);

    require(registryToken.transferFrom(msg.sender, this, deposit));
    challengePeriodEnd = now + registry.db().getUIntValue(challengePeriodDurationKey);
    creator = _creator;

    version = _version;

    registry.fireRegistryEntryEvent("constructed", version);
  }

  function createChallenge(
    address _challenger,
    bytes _challengeMetaHash
  )
  public
  notEmergency
  onlyConstructed
  {
    require(isChallengePeriodActive());
    require(!wasChallenged());
    require(registryToken.transferFrom(_challenger, this, deposit));

    challenge.challenger = _challenger;
    challenge.votingToken = MiniMeToken(registryToken.createCloneToken("", 18, "", 0, true));
    challenge.votingToken.changeController(0x0);
    uint commitDuration = registry.db().getUIntValue(commitPeriodDurationKey);
    uint revealDuration = registry.db().getUIntValue(revealPeriodDurationKey);
    uint deposit = registry.db().getUIntValue(depositKey);
    challenge.commitPeriodEnd = now + commitDuration;
    challenge.revealPeriodEnd = challenge.commitPeriodEnd + revealDuration;
    challenge.rewardPool = ((100 - registry.db().getUIntValue(challengeDispensationKey)) * deposit) / 100;
    challenge.metaHash = _challengeMetaHash;

    registry.fireRegistryEntryEvent("challengeCreated", version);
  }

  function commitVote(
    address _voter,
    bytes32 _secretHash
  )
  public
  notEmergency
  onlyConstructed
  {
    require(isVoteCommitPeriodActive());
    uint amount = challenge.votingToken.balanceOf(_voter);
    require(amount > 0);
    require(challenge.votingToken.transferFrom(_voter, this, amount));
    challenge.voter[_voter].secretHash = _secretHash;
    challenge.voter[_voter].amount = amount;
    registry.fireRegistryEntryEvent("voteCommited", version);
  }

  function revealVote(
    VoteOption _voteOption,
    string _salt
  )
  public
  notEmergency
  onlyConstructed
  {
    require(isVoteRevealPeriodActive());
    require(sha3(uint(_voteOption), _salt) == challenge.voter[msg.sender].secretHash);
    require(!hasVoterRevealed(msg.sender));

    challenge.voter[msg.sender].revealedOn = now;
    uint amount = challenge.voter[msg.sender].amount;
    challenge.voter[msg.sender].voteOption = _voteOption;
    if (_voteOption == VoteOption.VoteFor) {
      challenge.votesFor += amount;
    } else if (_voteOption == VoteOption.VoteAgainst) {
      challenge.votesAgainst += amount;
    } else {
      revert();
    }

    registry.fireRegistryEntryEvent("voteRevealed", version);
  }

  function claimVoterReward(
    address _voter
  )
  public
  notEmergency
  onlyConstructed
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

  function receiveApproval(
    address from,
    uint256 _amount,
    address _token,
    bytes _data)
  public
  {
    require(this.call(_data));
  }

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

  function winningVoteOption() public constant returns (VoteOption) {
    if (!isVoteRevealPeriodOver()) {
      return VoteOption.NoVote;
    }

    if (challenge.votesFor > challenge.votesAgainst) {
      return VoteOption.VoteFor;
    } else {
      return VoteOption.VoteAgainst;
    }
  }

  function winningVoteIsFor() public constant returns (bool) {
    return winningVoteOption() == VoteOption.VoteFor;
  }

  function winningVotesCount() public constant returns (uint) {
    VoteOption voteOption = winningVoteOption();

    if (voteOption == VoteOption.VoteFor) {
      return challenge.votesFor;
    } else if (voteOption == VoteOption.VoteAgainst) {
      return challenge.votesAgainst;
    } else {
      return 0;
    }
  }

  function voterReward(address _voter) public constant returns (uint) {
    uint tokensCount = winningVotesCount();
    uint voterAmount = 0;
    if (votedWinningVoteOption(_voter)) {
      voterAmount = challenge.voter[_voter].amount;
    }
    return (voterAmount * challenge.rewardPool) / tokensCount;
  }

  function votedWinningVoteOption(address _voter) public constant returns (bool) {
    return challenge.voter[_voter].voteOption == winningVoteOption();
  }

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