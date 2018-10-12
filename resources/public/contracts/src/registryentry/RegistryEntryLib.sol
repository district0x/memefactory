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
