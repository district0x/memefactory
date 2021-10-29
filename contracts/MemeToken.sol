// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC721/extensions/ERC721Enumerable.sol";
import "@openzeppelin/contracts/token/ERC721/extensions/ERC721URIStorage.sol";
import "./auth/DSAuth.sol";


// used to mint tokens coming from L2
interface IMintableERC721 {
  function mint(address user, uint256 tokenId) external;
  function mint(address user, uint256 tokenId, bytes calldata metaData) external;
  function exists(uint256 tokenId) external view returns (bool);
}


/**
 * @title Token of a Meme. Single ERC721 instance represents all memes/cards
 */
contract MemeToken is ERC721URIStorage, ERC721Enumerable, IMintableERC721, DSAuth {

  constructor ()
  ERC721("MemeToken", "MEME")
  {
  }

  function setTokenMetadata(uint256 _tokenId, bytes calldata _metadata) internal {
    // This function decodes metadata obtained from L2
    string memory uri = abi.decode(_metadata, (string));
    _setTokenURI(_tokenId, uri);
  }

  function encodeTokenMetadata(uint256 tokenId) public view virtual returns (bytes memory) {
    return abi.encode(tokenURI(tokenId));
  }

  function mint(address _to, uint256 _tokenId, bytes calldata _metaData)
  auth
  override
  external
  {
    _mint(_to, _tokenId);
    setTokenMetadata(_tokenId, _metaData);
  }

  function mint(address _to, uint256 _tokenId)
  auth
  override
  external
  {
    _mint(_to, _tokenId);
  }

  function exists(uint256 tokenId) external view override returns (bool) {
    return super._exists(tokenId);
  }

  function safeTransferFromMulti(
    address _from,
    address _to,
    uint256[] memory _tokenIds,
    bytes memory _data
  ) public {
    for (uint i = 0; i < _tokenIds.length; i++) {
      safeTransferFrom(_from, _to, _tokenIds[i], _data);
    }
  }

  // methods to resolve multiple inheritance

  function _beforeTokenTransfer(address from, address to, uint256 tokenId)
  internal
  override(ERC721, ERC721Enumerable)
  {
    super._beforeTokenTransfer(from, to, tokenId);
  }

  function _burn(uint256 tokenId) internal override(ERC721, ERC721URIStorage) {
    super._burn(tokenId);
  }

  function tokenURI(uint256 tokenId)
  public
  view
  override(ERC721, ERC721URIStorage)
  returns (string memory)
  {
    return super.tokenURI(tokenId);
  }

  function supportsInterface(bytes4 interfaceId)
  public
  view
  override(ERC721, ERC721Enumerable)
  returns (bool)
  {
    return super.supportsInterface(interfaceId);
  }
}
