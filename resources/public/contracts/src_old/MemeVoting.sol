pragma solidity ^0.4.18;

import "./minime/MiniMeToken.sol";
import "./libraries/Convert.sol";

contract MemeVoting is ApproveAndCallFallBack {

  struct Poll {
    uint commitEndDate;     /// expiration date of commit period for poll
    uint revealEndDate;     /// expiration date of reveal period for poll
    uint votesFor;          /// tally of votes supporting proposal
    uint votesAgainst;      /// tally of votes countering proposal
  }

  mapping(address => Poll) public pollMap; // maps poll voting token address to Poll struct

  /**
    @dev Initiates a poll with canonical configured parameters at pollID emitted by PollCreated event
    @param _commitDuration Length of desired commit period in seconds
    @param _revealDuration Length of desired reveal period in seconds
    */
  function startPoll(uint _commitDuration, uint _revealDuration, uint _parentToken) public returns (address pollTokenAddress) {
    address votingToken = MiniMeToken(_parentToken).createCloneToken(
      "MemeFactory MemeVoting Token",
      18,
      "MFVT",
      0,
      true
    );

    pollMap[votingToken] = Poll({
      commitEndDate : block.timestamp + _commitDuration,
      revealEndDate : block.timestamp + _commitDuration + _revealDuration,
      votesFor : 0,
      votesAgainst : 0
      });

    return votingToken;
  }

  function receiveApproval(address from, uint256 _amount, address _token, bytes _data) public {
    bytes32 secretHash = Convert.bytesToBytes32(_data, 0);



  }
}
