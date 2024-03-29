// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./RegistryEntry.sol";
import "./MemeToken.sol";
import "./DistrictConfig.sol";
import "./math/SafeMath.sol";

/**
 * @title Contract created for each submitted Meme into the MemeFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a meme image and meta data.
 * It also contains state and logic for handling initial meme offering.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract Meme is RegistryEntry {
  using SafeMath for uint;

  DistrictConfig private constant districtConfig = DistrictConfig(0xABCDabcdABcDabcDaBCDAbcdABcdAbCdABcDABCd);
  MemeToken private constant memeToken = MemeToken(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);
  bytes private metaHash;
  uint private tokenIdStart;
  uint private totalSupply;
  uint private totalMinted;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders pointing into single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _creator Creator of a meme
   * @param _version Version of Meme contract
   * @param _metaHash IPFS hash of meta data related to a meme
   * @param _totalSupply This meme's token total supply
   */
  function construct(
                     address _creator,
                     uint _version,
                     bytes memory _metaHash,
                     uint _totalSupply
                     )
    external
  {
    super.construct(_creator, _version);

    require(_totalSupply > 0);
    require(_totalSupply <= registry.db().getUIntValue(registry.maxTotalSupplyKey()));

    totalSupply = _totalSupply;
    metaHash = _metaHash;

    registry.fireMemeConstructedEvent(version,
                                      _creator,
                                      metaHash,
                                      totalSupply,
                                      deposit,
                                      challenge.challengePeriodEnd);
  }

  /**
   * @dev Transfers deposit to deposit collector
   * Must be callable only for whitelisted registry entries
   */
  function transferDeposit()
    external
    notEmergency
    onlyWhitelisted
  {
    require(registryToken.transfer(districtConfig.depositCollector(), deposit));

  }

  function mint(uint _amount)
    public
    notEmergency
    onlyWhitelisted
  {
    uint restSupply = totalSupply.sub(totalMinted);
    if (_amount == 0 || _amount > restSupply) {
      _amount = restSupply;
    }

    require(_amount > 0);

    tokenIdStart = registry.nextTokenId();
    uint tokenIdEnd = tokenIdStart.add(_amount);
    bytes memory metadata = buildMetadata();
    for (uint i = tokenIdStart; i < tokenIdEnd; i++) {
      memeToken.mint(creator, i, metadata);
      totalMinted = totalMinted + 1;
    }

    registry.fireMemeMintedEvent(version,
                                 creator,
                                 tokenIdStart,
                                 tokenIdEnd-1,
                                 totalMinted);
  }

  function buildMetadata() private view returns (bytes memory) {
    string memory uri = string(abi.encodePacked("ipfs://", string(metaHash)));
    return abi.encode(uri);
  }

  function loadMeme() external view returns (bytes memory,
                                                 uint,
                                                 uint,
                                                 uint){
    return(metaHash,
           totalSupply,
           totalMinted,
           tokenIdStart);
  }

}
