pragma solidity ^0.4.24;

library Vote {

  enum VoteOption {NoVote, VoteFor, VoteAgainst}
  
  struct Vote {
    bytes32 secretHash;
    VoteOption option;
    uint amount;
    uint revealedOn;
    uint claimedRewardOn;
    uint reclaimedVoteAmountOn;
  }








}
