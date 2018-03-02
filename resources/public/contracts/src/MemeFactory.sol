pragma solidity ^0.4.18;

import "RegistryEntryFactory.sol";
import "Meme.sol";

/**
 * @title Factory contract for creating Meme contracts
 *
 * @dev Users submit new memes into this contract.
 */

contract MemeFactory is RegistryEntryFactory {
  uint public constant version = 1;

  function MemeFactory(Registry _registry, MiniMeToken _registryToken)
  RegistryEntryFactory(_registry, _registryToken)
  {}

  /**
   * @dev Creates new Meme forwarder contract and add it into the registry
   * It initializes forwarder contract with initial state. For comments on each param, see Meme::construct
   */
  function createMeme(
    address _creator,
    string _name,
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
      _metaHash,
      _totalSupply,
      _startPrice
    );
  }
}