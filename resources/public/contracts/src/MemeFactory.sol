pragma solidity ^0.4.18;

import "RegistryEntryFactory.sol";
import "Meme.sol";

contract MemeFactory is RegistryEntryFactory {
  uint public constant version = 1;

  function MemeFactory(Registry _registry, MiniMeToken _registryToken)
  RegistryEntryFactory(_registry, _registryToken)
  {}

  function createMeme(
    address _creator,
    string _name,
    bytes _imageHash,
    bytes _metaHash,
    uint _totalSupply,
    uint _startPrice
  )
  public
  {
    Meme meme = Meme(createRegistryEntry(_creator));

    meme.construct(
      _creator,
      version,
      _name,
      _imageHash,
      _metaHash,
      _totalSupply,
      _startPrice
    );
  }
}