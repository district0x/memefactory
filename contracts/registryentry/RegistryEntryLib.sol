// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "../Registry.sol";
import "../math/SafeMath.sol";

library RegistryEntryLib {

  using SafeMath for uint;

  enum VoteOption {NoVote, VoteFor, VoteAgainst}

  enum Status {ChallengePeriod, CommitPeriod, RevealPeriod, Blacklisted, Whitelisted}

  struct Vote {
    bytes32 secretHash;
    VoteOption option;
    uint amount;
    uint revealedOn;
    uint claimedRewardOn;
    uint reclaimedVoteAmountOn;
  }

  struct Challenge {
    address challenger;
    uint rewardPool;
    bytes metaHash;
    uint commitPeriodEnd;
    uint revealPeriodEnd;
    uint challengePeriodEnd;
    uint votesFor;
    uint votesAgainst;
    uint claimedRewardOn;
    mapping(address => Vote) vote;
  }

  // External functions

  /**
   * @dev Returns date when registry entry was whitelisted
   * Since this doesn't happen with any transaction, it's either reveal or challenge period end

   * @return UNIX time of whitelisting
   */
  /* function whitelistedOn(Challenge storage self) */
  /*   external */
  /*   constant */
  /*   returns (uint) { */
  /*   if (!isWhitelisted()) { */
  /*     return 0; */
  /*   } */
  /*   if (self.wasChallenged()) { */
  /*     return self.revealPeriodEnd; */
  /*   } else { */
  /*     return challengePeriodEnd; */
  /*   } */
  /* } */

  // Internal functions

  function isVoteRevealPeriodActive(Challenge storage self)
    internal
    view
    returns (bool) {
    return !isVoteCommitPeriodActive(self) && block.timestamp <= self.revealPeriodEnd;
  }

  function isVoteRevealed(Challenge storage self, address _voter)
    internal
    view
    returns (bool) {
    return self.vote[_voter].revealedOn > 0;
  }

  function isVoteRewardClaimed(Challenge storage self, address _voter)
    internal
    view
    returns (bool) {
    return self.vote[_voter].claimedRewardOn > 0;
  }

  function isVoteAmountReclaimed(Challenge storage self, address _voter)
    internal
    view
    returns (bool) {
    return self.vote[_voter].reclaimedVoteAmountOn > 0;
  }

  function isChallengeRewardClaimed(Challenge storage self)
    internal
    view
    returns (bool) {
    return self.claimedRewardOn > 0;
  }

  function isChallengePeriodActive(Challenge storage self)
    internal
    view
    returns (bool) {
    return block.timestamp <= self.challengePeriodEnd;
  }

  function isWhitelisted(Challenge storage self)
    internal
    view
    returns (bool) {
    return status(self) == Status.Whitelisted;
  }

  function isVoteCommitPeriodActive(Challenge storage self)
    internal
    view
    returns (bool) {
    return block.timestamp <= self.commitPeriodEnd;
  }

  function isVoteRevealPeriodOver(Challenge storage self)
    internal
    view
    returns (bool) {
    return self.revealPeriodEnd > 0 && block.timestamp > self.revealPeriodEnd;
  }

  /**
   * @dev Returns whether VoteFor is winning vote option
   *
   * @return True if VoteFor is winning option
   */
  function isWinningOptionVoteFor(Challenge storage self)
    internal
    view
    returns (bool) {
    return winningVoteOption(self) == VoteOption.VoteFor;
  }

  function hasVoted(Challenge storage self, address _voter)
    internal
    view
    returns (bool) {
    return self.vote[_voter].amount != 0;
  }

  function wasChallenged(Challenge storage self)
    internal
    view
    returns (bool) {
    return self.challenger != address(0);
  }

  /**
   * @dev Returns whether voter voted for winning vote option
   * @param _voter Address of a voter
   *
   * @return True if voter voted for a winning vote option
   */
  function votedWinningVoteOption(Challenge storage self, address _voter)
    internal
    view
    returns (bool) {
    return self.vote[_voter].option == winningVoteOption(self);
  }

  /**
   * @dev Returns current status of a registry entry

   * @return Status
   */
  function status(Challenge storage self)
    internal
    view
    returns (Status) {
    if (isChallengePeriodActive(self) && !wasChallenged(self)) {
      return Status.ChallengePeriod;
    } else if (isVoteCommitPeriodActive(self)) {
      return Status.CommitPeriod;
    } else if (isVoteRevealPeriodActive(self)) {
      return Status.RevealPeriod;
    } else if (isVoteRevealPeriodOver(self)) {
      if (isWinningOptionVoteFor(self)) {
        return Status.Whitelisted;
      } else {
        return Status.Blacklisted;
      }
    } else {
      return Status.Whitelisted;
    }
  }

  /**
   * @dev Returns token reward amount belonging to a challenger
   *
   * @return Amount of token
   */
  function challengeReward(Challenge storage self, uint deposit)
    internal
    view
    returns (uint) {
    return deposit.add(deposit.sub(self.rewardPool));
  }

  /**
   * @dev Returns token reward amount belonging to a voter for voting for a winning option
   * @param _voter Address of a voter
   *
   * @return Amount of tokens
   */
  function voteReward(Challenge storage self, address _voter)
    internal
    view
    returns (uint) {
    uint winningAmount = winningVotesAmount(self);
    uint voterAmount = 0;

    if (!votedWinningVoteOption(self, _voter)) {
      return voterAmount;
    }

    voterAmount = self.vote[_voter].amount;
    return (voterAmount.mul(self.rewardPool)) / winningAmount;
  }

  /**
   * @dev Returns true when parameter key is in a whitelisted set and the parameter
   * value is within the allowed set of values.
   */
  function isChangeAllowed(Registry registry, bytes32 record, uint _value)
    internal
    view
    returns (bool) {

      if(record == registry.challengePeriodDurationKey() || record == registry.commitPeriodDurationKey() ||
         record == registry.revealPeriodDurationKey() || record == registry.depositKey()) {
        if(_value > 0) {
          return true;
        }
      }

      if(record == registry.challengeDispensationKey() ||
         record == registry.maxTotalSupplyKey()) {
        if (_value >= 0 && _value <= 100) {
          return true;
        }
      }

      // see MemeAuction.sol startAuction
      if(record == registry.maxAuctionDurationKey()) {
        if(_value >= 1 minutes) {
          return true;
        }
      }

    return false;
  }

  // Private functions

  function isBlacklisted(Challenge storage self)
    private
    view
    returns (bool) {
    return status(self) == Status.Blacklisted;
  }

  /**
   * @dev Returns winning vote option in held voting
   *
   * @return Winning vote option
   */
  function winningVoteOption(Challenge storage self)
    private
    view
    returns (VoteOption) {
    if (!isVoteRevealPeriodOver(self)) {
      return VoteOption.NoVote;
    }

    if (self.votesAgainst < self.votesFor) {
      return VoteOption.VoteFor;
    } else {
      return VoteOption.VoteAgainst;
    }
  }

  /**
   * @dev Returns amount of votes for winning vote option
   *
   * @return Amount of votes
   */
  function winningVotesAmount(Challenge storage self)
    private
    view
    returns (uint) {
    VoteOption voteOption = winningVoteOption(self);

    if (voteOption == VoteOption.VoteFor) {
      return self.votesFor;
    } else if (voteOption == VoteOption.VoteAgainst) {
      return self.votesAgainst;
    } else {
      return 0;
    }
  }

}
