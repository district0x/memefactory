// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./MemeToken.sol";


/**
 * @title Token of a Meme in L2
 */
contract MemeTokenChild is MemeToken {

  constructor () MemeToken()
  {}

  /**
  * @dev burn only allowed for custom bridge child contracts after withdraw
  */
  function burn(uint256 tokenId) external auth {
    _burn(tokenId);
  }
}
