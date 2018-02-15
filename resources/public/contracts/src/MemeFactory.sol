pragma solidity ^0.4.18;

import "Registry.sol";
import "Meme.sol";
import "forwarder/Forwarder.sol";

contract MemeFactory {
  Registry public registry;
  uint public constant version = 1;

  function MemeFactory(Registry _registry) {
    registry = _registry;
  }

  function createMeme(
    string _name,
    bytes _imageHash,
    bytes _metaHash,
    uint _totalSupply,
    uint _startPrice
  )
  public
  {
    address meme = Meme(new Forwarder());
    Meme(meme).construct(
      version,
      msg.sender,
      _name,
      _imageHash,
      _metaHash,
      _totalSupply,
      _startPrice
    );
    registry.addRegistryEntry(meme);
  }
}