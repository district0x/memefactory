pragma solidity ^0.4.18;

import "Registry.sol";
import "forwarder/Forwarder.sol";
import "storage/EternalStorage.sol";
import "token/minime/MiniMeToken.sol";

contract RegistryEntry is ApproveAndCallFallBack {
  Registry public constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  EternalStorage public constant parametrizer = EternalStorage(0xDeEDdeeDDEeDDEEdDEedDEEdDEeDdEeDDEEDDeed);
  MiniMeToken public constant registryToken = MiniMeToken(0xDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaD);

  bytes32 public constant applicationPeriodDurationKey = "";
  bytes32 public constant commitPeriodDurationKey = "";
  bytes32 public constant revealPeriodDurationKey = "";
  bytes32 public constant depositKey = "";
  bytes32 public constant challengeDispensationKey = "";

  uint public version;

  enum Status {ApplicationPeriod, CommitVotePeriod, RevealVotePeriod, VotedAgainst, Whitelisted}

  struct Entry {
    uint applicationEndDate;      // Expiration date of apply stage
    address creator;              // Owner of Listing
    uint deposit;                 // Number of tokens deposited when applying
  }

  struct Challenge {
    MiniMeToken votingToken;    // Address of MiniMeToken fork
    uint rewardPool;            // (remaining) Pool of tokens to be distributed to winning voters
    address challenger;         // Owner of Challenge
    bytes metaHash;
    uint resolvedOn;            // Indication of if challenge is resolved
    uint commitEndDate;         /// expiration date of commit period for poll
    uint revealEndDate;         /// expiration date of reveal period for poll
    uint votesFor;              /// tally of votes supporting entry
    uint votesAgainst;          /// tally of votes countering entry
    mapping(address => Voter) voter;
  }

  struct Voter {
    uint claimedRewardOn;
    uint revealedOn;
    bytes32 secretHash;
    VoteOption voteOption;
    uint amount;
  }

  enum VoteOption {NoVote, VoteFor, VoteAgainst}

  Entry public entry;
  Challenge public challenge;

  modifier notEmergency() {
    require(!registry.isEmergency());
    _;
  }

  modifier onlyConstructed() {
    require(entry.applicationEndDate > 0);
    _;
  }

  function construct(
    uint _version,
    address _creator
  )
  public
  {
    require(entry.applicationEndDate == 0);

    uint deposit = parametrizer.getUIntValue(depositKey);
    require(registryToken.transferFrom(_creator, this, deposit));
    entry.applicationEndDate = now + parametrizer.getUIntValue(applicationPeriodDurationKey);
    entry.deposit = deposit;
    entry.creator = _creator;

    version = _version;

    registry.fireRegistryEntryEvent("construct", _version);
  }

  function createChallenge(
    bytes _challengeMetaHash
  )
  public
  notEmergency
  onlyConstructed
  {
    require(isApplicationPeriodActive());
    require(!wasChallenged());
    require(registryToken.transferFrom(msg.sender, this, entry.deposit));

    challenge.challenger = msg.sender;
    challenge.votingToken = MiniMeToken(registryToken.createCloneToken("", 18, "", 0, true));
    uint commitDuration = parametrizer.getUIntValue(commitPeriodDurationKey);
    uint revealDuration = parametrizer.getUIntValue(revealPeriodDurationKey);
    uint deposit = parametrizer.getUIntValue(depositKey);
    challenge.commitEndDate = now + commitDuration;
    challenge.revealEndDate = challenge.commitEndDate + revealDuration;
    challenge.rewardPool = ((100 - parametrizer.getUIntValue(challengeDispensationKey)) * deposit) / 100;
    challenge.metaHash = _challengeMetaHash;

    registry.fireRegistryEntryEvent("challengeCreated", version);
  }

  function commitVote(
    bytes32 _secretHash
  )
  public
  notEmergency
  onlyConstructed
  {
    require(isVoteCommitPeriodActive());
    uint amount = challenge.votingToken.balanceOf(msg.sender);
    require(challenge.votingToken.transferFrom(msg.sender, this, amount));
    challenge.voter[msg.sender].secretHash = _secretHash;
    challenge.voter[msg.sender].amount = amount;
    registry.fireRegistryEntryEvent("voteCommited", version);
  }

  function revealVote(
    VoteOption _voteOption,
    uint _salt
  )
  public
  notEmergency
  onlyConstructed
  {
    require(isVoteRevealPeriodActive());
    require(sha3(_voteOption, _salt) == challenge.voter[msg.sender].secretHash);
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
    require(registryToken.transferFrom(this, _voter, reward));

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
    this.call(_data);
  }

  function status() public constant returns (Status) {
    if (isApplicationPeriodActive() && !wasChallenged()) {
      return Status.ApplicationPeriod;
    } else if (isVoteCommitPeriodActive()) {
      return Status.CommitVotePeriod;
    } else if (isVoteRevealPeriodActive()) {
      return Status.RevealVotePeriod;
    } else if (isVoteRevealPeriodOver()) {
      if (winningVoteIsFor()) {
        return Status.Whitelisted;
      } else {
        return Status.VotedAgainst;
      }
    } else {
      return Status.Whitelisted;
    }
  }

  function isApplicationPeriodActive() public constant returns (bool) {
    return now <= entry.applicationEndDate;
  }

  function isWhitelisted() public constant returns (bool) {
    return status() == Status.Whitelisted;
  }

  function whitelistedOn() public constant returns (uint) {
    if (!isWhitelisted()) {
      return 0;
    }
    if (wasChallenged()) {
      return challenge.revealEndDate;
    } else {
      return entry.applicationEndDate;
    }
  }

  function wasChallenged() public constant returns (bool) {
    return challenge.challenger != 0x0;
  }

  function isVoteCommitPeriodActive() public constant returns (bool) {
    return now <= challenge.commitEndDate;
  }

  function isVoteRevealPeriodActive() public constant returns (bool) {
    return !isVoteCommitPeriodActive() && now <= challenge.revealEndDate;
  }

  function isVoteRevealPeriodOver() public constant returns (bool) {
    return challenge.revealEndDate > 0 && now > challenge.revealEndDate;
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
}