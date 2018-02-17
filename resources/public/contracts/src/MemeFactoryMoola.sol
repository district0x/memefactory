pragma solidity ^0.4.18;

import "./token/minime/MiniMeToken.sol";

contract MemeFactoryMoola is MiniMeToken {
  function MemeFactoryMoola(address _tokenFactory, uint _mintedAmount)
  MiniMeToken(
    _tokenFactory,
    0x0,
    0,
    "MemeFactory Moola",
    18,
    "MFM",
    true
  )
  {
    generateTokens(msg.sender, _mintedAmount);
    changeController(0x0);
  }
}