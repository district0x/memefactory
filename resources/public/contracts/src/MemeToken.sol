pragma solidity ^0.4.18;

import "token/ERC721Token.sol";
import "Registry.sol";
import "auth/DSAuth.sol";

/**
 * @title Token of a Meme. Contract is deployed with each Meme submission.
 *
 * @dev Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract MemeToken is ERC721Token {
  Registry public registry;
  // Optional mapping for token URIs
  mapping(uint256 => address) internal registryEntries;

  modifier onlyRegistryEntry() {
    require(registry.isRegistryEntry(msg.sender));
    _;
  }

  function MemeToken(Registry _registry)
  ERC721Token("MemeToken", "MEME")
  {
    registry = _registry;
  }

  /**
  * @dev Returns a registry entry for a given token ID
  * @dev Throws if the token ID does not exist. May return an empty string.
  * @param _tokenId uint256 ID of the token to query
  */
  function registryEntry(uint256 _tokenId) public view returns (address) {
    require(exists(_tokenId));
    return registryEntries[_tokenId];
  }

  function mint(address _to, uint256 _tokenId)
  onlyRegistryEntry
  public
  {
    super._mint(_to, _tokenId);
    registryEntries[_tokenId] = msg.sender;
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