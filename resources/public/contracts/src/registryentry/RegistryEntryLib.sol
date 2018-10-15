pragma solidity ^0.4.24;

import "math/SafeMath.sol";

library RegistryEntryLib {

  using SafeMath for uint;

  enum VoteOption {NoVote, VoteFor, VoteAgainst}

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
    uint voteQuorum;
    uint rewardPool;
    bytes metaHash;
    uint commitPeriodEnd;
    uint revealPeriodEnd;
    uint votesFor;
    uint votesAgainst;
    uint claimedRewardOn;
    mapping(address => Vote) vote;
  }

  function wasChallenged(Challenge storage self)
    internal
    constant
    returns (bool) {
    return self.challenger != 0x0;
  }

  /**
   * @dev Returns token reward amount belonging to a challenger
   *
   * @return Amount of token
   */
  function challengeReward(Challenge storage self, uint deposit)
    internal
    constant
    returns (uint) {
    return deposit.sub(self.rewardPool);
  }

  function isVoteCommitPeriodActive(Challenge storage self)
    internal
    constant
    returns (bool) {
    return now <= self.commitPeriodEnd;
  }

  function isVoteRevealPeriodOver(Challenge storage self)
    internal
    constant
    returns (bool) {
    return self.revealPeriodEnd > 0 && now > self.revealPeriodEnd;
  }

  /**
   * @dev Returns winning vote option in held voting according to vote quorum
   * If voteQuorum is 50, any majority of votes will win
   * If voteQuorum is 24, only 25 votes out of 100 is enough to VoteFor be winning option
   *
   * @return Winning vote option
   */
  function winningVoteOption(Challenge storage self)
    internal
    constant
    returns (VoteOption) {
    if (!isVoteRevealPeriodOver(self)) {
      return VoteOption.NoVote;
    }

    if (self.votesFor.mul(100) > self.voteQuorum.mul(self.votesFor.add(self.votesAgainst))) {
      return VoteOption.VoteFor;
    } else {
      return VoteOption.VoteAgainst;
    }
  }

  /**
   * @dev Returns whether voter voted for winning vote option
   * @param _voter Address of a voter
   *
   * @return True if voter voted for a winning vote option
   */
  function votedWinningVoteOption(Challenge storage self, address _voter)
    internal
    constant
    returns (bool) {
    return self.vote[_voter].option == winningVoteOption(self);
  }

  /**
   * @dev Returns whether VoteFor is winning vote option
   *
   * @return True if VoteFor is winning option
   */
  function isWinningOptionVoteFor(Challenge storage self)
    internal
    constant
    returns (bool) {
    return winningVoteOption(self) == VoteOption.VoteFor;
  }

  /**
   * @dev Returns amount of votes for winning vote option
   *
   * @return Amount of votes
   */
  function winningVotesAmount(Challenge storage self)
    private
    constant
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

  
  /**
   * @dev Returns token reward amount belonging to a voter for voting for a winning option
   * @param _voter Address of a voter
   *
   * @return Amount of tokens
   */
  function voteReward(Challenge storage self, address _voter)
    internal
    constant
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


}
