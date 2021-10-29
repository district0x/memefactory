// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./RegistryEntryFactory.sol";
import "./Meme.sol";
import "./MemeToken.sol";

/**
 * @title Factory contract for creating Meme contracts
 *
 * @dev Users submit new memes into this contract.
 */

contract MemeFactory is RegistryEntryFactory {
  uint public constant version = 1;
  MemeToken public memeToken;

  constructor (Registry _registry, MiniMeToken _registryToken, MemeToken _memeToken)
  RegistryEntryFactory(_registry, _registryToken)
  {
    memeToken = _memeToken;
  }

  /**
   * @dev Creates new Meme forwarder contract and add it into the registry
   * It initializes forwarder contract with initial state. For comments on each param, see Meme::construct
   */
  function createMeme(
    address _creator,
    bytes memory _metaHash,
    uint _totalSupply
  )
  public
  {
    Meme meme = Meme(createRegistryEntry(_creator));

    meme.construct(
      _creator,
      version,
      _metaHash,
      _totalSupply
    );
  }
}
