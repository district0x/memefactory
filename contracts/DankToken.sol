// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./token/minime/MiniMeToken.sol";

/**
 * @title Token used for curation of MemeFactory TCR
 *
 * @dev Standard MiniMe Token with pre-minted supply and with dead controller.
 */

contract DankToken is MiniMeToken {
  constructor (address _tokenFactory, uint _mintedAmount)
  MiniMeToken(
    _tokenFactory,
    payable(address(0)),
    0,
    "Dank Token",
    18,
    "DANK",
    true
  )
  {
    generateTokens(msg.sender, _mintedAmount);
    changeController(address(0));
  }
}