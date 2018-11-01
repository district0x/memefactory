pragma solidity ^0.4.24;

import "./token/ERC721Token.sol";
import "./Registry.sol";
import "./auth/DSAuth.sol";

/**
 * @title Token of a Meme. Contract is deployed with each Meme submission.
 *
 * @dev Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract MemeToken is ERC721Token {
  Registry public registry;

  modifier onlyRegistryEntry() {
    require(registry.isRegistryEntry(msg.sender),"MemeToken: onlyRegistryEntry failed");
    _;
  }

  function MemeToken(Registry _registry)
  ERC721Token("MemeToken", "MEME")
  {
    registry = _registry;
  }

  function mint(address _to, uint256 _tokenId)
  onlyRegistryEntry
  public
  {
    super._mint(_to, _tokenId);
    tokenURIs[_tokenId] = msg.sender;
  }

  function safeTransferFromMulti(
    address _from,
    address _to,
    uint256[] _tokenIds,
    bytes _data
  ) {
    for (uint i = 0; i < _tokenIds.length; i++) {
      safeTransferFrom(_from, _to, _tokenIds[i], _data);
    }
  }
}
