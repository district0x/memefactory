pragma solidity ^0.4.18;

import "./token/minime/MiniMeToken.sol";

contract DankToken is MiniMeToken {
  function DankToken(address _tokenFactory, uint _mintedAmount)
  MiniMeToken(
    _tokenFactory,
    0x0,
    0,
    "Dank Token",
    18,
    "DANK",
    true
  )
  {
    generateTokens(msg.sender, _mintedAmount);
    changeController(0x0);
  }
}