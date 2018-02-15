pragma solidity ^0.4.18;

import "Registry.sol";
import "forwarder/Forwarder.sol";
import "RegistryEntryToken.sol";
import "storage/EternalStorage.sol";
import "token/minime/MiniMeToken.sol";
import "token/MintableToken.sol";

contract RegistryEntry is ApproveAndCallFallBack {
  Registry public constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  EternalStorage public constant parametrizer = EternalStorage(0xDeEDdeeDDEeDDEEdDEedDEEdDEeDdEeDDEEDDeed);
  MiniMeToken public constant registryToken = MiniMeToken(0xDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaD);
  uint public version;

  enum Status {ApplicationPeriod, CommitVotePeriod, RevealVotePeriod, VotedAgainst, Whitelisted}

  struct Entry {
    uint applicationEndDate;      // Expiration date of apply stage
    address creator;              // Owner of Listing
    uint deposit;                 // Number of tokens deposited when applying
  }

  struct Sale {
    uint startPrice;
    uint64 duration;
  }

  struct Meta {
    MintableToken token;
    bytes imageHash;
    bytes metaHash;
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
  Sale public sale;
  Meta public meta;
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
    address _creator,
    string _name,
    bytes _imageHash,
    bytes _metaHash,
    uint _totalSupply,
    uint _startPrice
  )
  public
  {
    require(entry.applicationEndDate == 0);

    meta.token = MintableToken(new Forwarder());
    MemeToken(meta.token).construct(_name);
    MemeToken(meta.token).mint(this, _totalSupply);
    MintableToken(meta.token).finishMinting();

    meta.imageHash = _imageHash;
    meta.metaHash = _metaHash;

    require(_startPrice <= parametrizer.getUIntValue(sha3("registryEntryMaxStartPrice")));
    sale = Sale(_startPrice, uint64(parametrizer.getUIntValue(sha3("registryEntrySaleDuration"))));

    uint deposit = parametrizer.getUIntValue(sha3("registryEntryDeposit"));
    require(registryToken.transferFrom(_creator, this, deposit));
    entry.applicationEndDate = now + parametrizer.getUIntValue(sha3("registryEntryApplicationDuration"));
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
    uint commitDuration = parametrizer.getUIntValue(sha3("registryEntryChallengeCommitDuration"));
    uint revealDuration = parametrizer.getUIntValue(sha3("registryEntryChallengeRevealDuration"));
    uint deposit = parametrizer.getUIntValue(sha3("registryEntryDeposit"));
    challenge.commitEndDate = now + commitDuration;
    challenge.revealEndDate = challenge.commitEndDate + revealDuration;
    challenge.rewardPool = ((100 - parametrizer.getUIntValue(sha3("registryEntryChallengeDispensation"))) * deposit) / 100;
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

  function buy(uint _amount)
  payable
  public
  notEmergency
  onlyConstructed
  {
    require(isWhitelisted());
    require(_amount > 0);

    var price = currentPrice() * _amount;

    require(msg.value >= price);
    require(meta.token.transfer(msg.sender, _amount));
    entry.creator.transfer(msg.value);
    if (msg.value > price) {
      msg.sender.transfer(msg.value - price);
    }
    registry.fireRegistryEntryEvent("buy", version);
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

  function currentPrice() constant returns (uint) {
    uint secondsPassed = 0;
    uint listedOn = whitelistedOn();

    if (now > listedOn && listedOn > 0) {
      secondsPassed = now - listedOn;
    }

    return computeCurrentPrice(
      sale.startPrice,
      sale.duration,
      secondsPassed
    );
  }

  function computeCurrentPrice(uint _startPrice, uint _duration, uint _secondsPassed) constant returns (uint) {
    if (_secondsPassed >= _duration) {
      // We've reached the end of the dynamic pricing portion
      // of the auction, just return the end price.
      return 0;
    } else {
      // Starting price can be higher than ending price (and often is!), so
      // this delta can be negative.
      int totalPriceChange = 0 - int(_startPrice);

      // This multiplication can't overflow, _secondsPassed will easily fit within
      // 64-bits, and totalPriceChange will easily fit within 128-bits, their product
      // will always fit within -bits.
      int currentPriceChange = totalPriceChange * int(_secondsPassed) / int(_duration);

      // currentPriceChange can be negative, but if so, will have a magnitude
      // less that _startingPrice. Thus, this result will always end up positive.
      int currentPrice = int(_startPrice) + currentPriceChange;

      return uint(currentPrice);
    }
  }

}