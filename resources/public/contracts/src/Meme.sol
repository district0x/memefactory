pragma solidity ^0.4.24;

import "RegistryEntry.sol";
import "MemeToken.sol";
import "proxy/Forwarder.sol";
import "./DistrictConfig.sol";

/**
 * @title Contract created for each submitted Meme into the MemeFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a meme image and meta data.
 * It also contains state and logic for handling initial meme offering.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract Meme is RegistryEntry {

  DistrictConfig public constant districtConfig = DistrictConfig(0xABCDabcdABcDabcDaBCDAbcdABcdAbCdABcDABCd);
  MemeToken public constant memeToken = MemeToken(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);
  bytes public metaHash;
  uint public tokenIdStart;
  uint public totalSupply;
  uint public totalMinted;

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
    bytes _metaHash,
    uint _totalSupply
  )
  external
  {
    super.construct(_creator, _version);

    require(_totalSupply > 0, "Meme: totalSupply should be > 0");
    require(_totalSupply <= registry.db().getUIntValue(registry.maxTotalSupplyKey()), "Meme: totalSupply shoud be < maxTotalSupply");

    totalSupply = _totalSupply;
    metaHash = _metaHash;
  }

  /**
   * @dev Transfers deposit to deposit collector
   * Must be callable only for whitelisted unchallenged registry entries
   */
  function transferDeposit()
  external
  notEmergency
  onlyWhitelisted
  {
    require(!challenge.wasChallenged(), "Meme: Is challenged");
    require(registryToken.transfer(districtConfig.depositCollector(), deposit), "Meme: can't transfer deposit");
    registry.fireRegistryEntryEvent("depositTransferred", version);
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
    require(_amount > 0, "Meme: Amount should be > 0");
    tokenIdStart = memeToken.totalSupply().add(1);
    uint tokenIdEnd = tokenIdStart.add(_amount);
    for (uint i = tokenIdStart; i < tokenIdEnd; i++) {
      memeToken.mint(creator, i);
      totalMinted = totalMinted + 1;
    }
    var eventData = new uint[](4);
    eventData[0] = uint(creator);
    eventData[1] = tokenIdStart;
    eventData[2] = tokenIdEnd - 1;
    eventData[3] = totalMinted;
    /* eventData[3] = bytesToUint(metaHash); */
    registry.fireRegistryEntryEvent("minted", version, eventData);
  }

  /**
   * @dev Returns all state related to this contract for simpler offchain access
   */
  function loadMeme()
    external
    constant
    returns (bytes, uint, uint, uint) {
    return (
    metaHash,
    totalSupply,
    totalMinted,
    tokenIdStart
    );
  }
}
