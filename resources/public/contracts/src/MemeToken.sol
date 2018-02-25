pragma solidity ^0.4.18;

import "token/MintableToken.sol";

/**
 * @title Token of a Meme. Contract is deployed with each Meme submission.
 *
 * @dev Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract MemeToken is MintableToken {
  string public name;
  uint8 public decimals = 0;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders into single instance of this contract,
   * therefore constructor must be called explicitly.
   * Must be callable only once

   * @param _name Name of the token
  */
  function construct(
    string _name
  ) {
    require(bytes(name).length == 0);
    require(bytes(_name).length != 0);
    name = _name;
    owner = msg.sender;
  }

  /**
   * @dev Returns symbol of a token
   * Meme tokens have same name as their symbol

   * @return Symbol of the token
   */
  function symbol() public constant returns (string) {
    return name;
  }
}