pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "MemeToken.sol";
import "forwarder/Forwarder.sol";

/**
 * @title Contract created for each submitted Meme into the MemeFactory TCR.
 *
 * @dev It extends base RegistryEntry with additional state for storing IPFS hashes for a meme image and meta data.
 * It also contains state and logic for handling initial meme offering.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract Meme is RegistryEntry {

  address public constant depositCollector = Registry(0xABCDabcdABcDabcDaBCDAbcdABcdAbCdABcDABCd);
  bytes32 public constant maxStartPriceKey = sha3("maxStartPrice");
  bytes32 public constant maxTotalSupplyKey = sha3("maxTotalSupply");
  bytes32 public constant saleDurationKey = sha3("saleDuration");
  MemeToken token;
  bytes imageHash;
  bytes metaHash;
  uint startPrice;
  uint saleDuration;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders pointing into single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _creator Creator of a meme
   * @param _version Version of Meme contract
   * @param _imageHash IPFS hash of meme image
   * @param _metaHash IPFS hash of meta data related to a meme
   * @param _totalSupply This meme's token total supply
   * @param _startPrice Start price for initial meme offering
   */
  function construct(
    address _creator,
    uint _version,
    string _name,
    bytes _imageHash,
    bytes _metaHash,
    uint _totalSupply,
    uint _startPrice
  )
  public
  {
    super.construct(_creator, _version);

    require(_totalSupply > 0);
    require(_totalSupply <= registry.db().getUIntValue(maxTotalSupplyKey));
    token = MemeToken(new Forwarder());
    token.construct(_name);
    token.mint(this, _totalSupply);
    token.finishMinting();

    imageHash = _imageHash;
    metaHash = _metaHash;

    require(_startPrice <= registry.db().getUIntValue(maxStartPriceKey));
    startPrice = _startPrice;
    saleDuration = registry.db().getUIntValue(saleDurationKey);
  }

  /**
   * @dev Transfers deposit to depositCollector
   * Must be callable only for whitelisted unchallenged registry entries
   */
  function transferDeposit()
  public
  notEmergency
  onlyWhitelisted
  {
    require(!wasChallenged());
    require(registryToken.transfer(depositCollector, deposit));
    registry.fireRegistryEntryEvent("depositTransferred", version);
  }

  /**
   * @dev Buys meme token from initial meme offering
   * Creator of a meme gets ETH paid for a meme token
   * If buyer sends more than is current price, extra ETH is sent back to the buyer

   * @param _amount Amount of meme token desired to buy
   */
  function buy(uint _amount)
  payable
  public
  notEmergency
  onlyWhitelisted
  {
    require(_amount > 0);

    var price = currentPrice().mul(_amount);

    require(msg.value >= price);
    require(token.transfer(msg.sender, _amount));
    creator.transfer(price);
    if (msg.value > price) {
      msg.sender.transfer(msg.value.sub(price));
    }
    registry.fireRegistryEntryEvent("buy", version);
  }

  /**
   * @dev Returns current price of a meme in initial meme offering.

   * @return Current price of a meme
   */
  function currentPrice() constant returns (uint) {
    uint secondsPassed = 0;
    uint listedOn = whitelistedOn();

    if (now > listedOn && listedOn > 0) {
      secondsPassed = now.sub(listedOn);
    }

    return computeCurrentPrice(
      startPrice,
      saleDuration,
      secondsPassed
    );
  }

  /**
   * @dev Calculates current price of decreasing-price style auction

   * @param _startPrice Start price of an auction
   * @param _duration Total duration of an auction in seconds
   * @param _secondsPassed Amount of seconds passed from the beginning of an auction

   * @return Calculated price
   */
  function computeCurrentPrice(uint _startPrice, uint _duration, uint _secondsPassed) constant returns (uint) {
    if (_secondsPassed >= _duration) {
      // We've reached the end of the dynamic pricing portion
      // of the auction, just return the end price.
      return 0;
    } else {
      // Starting price is higher than ending price, so this delta is negative.
      int totalPriceChange = 0 - int(_startPrice);

      // This multiplication can't overflow, _secondsPassed will easily fit within
      // 64-bits, and totalPriceChange will easily fit within 128-bits, their product
      // will always fit within -bits.
      int currentPriceChange = totalPriceChange * int(_secondsPassed) / int(_duration);

      // currentPriceChange can be negative, but if so, will have a magnitude
      // less that _startingPrice. Thus, this result will always end up positive.
      int currentPrice = int(_startPrice) + currentPriceChange;

      return uint(currentPrice);
    }
  }

  /**
   * @dev Returns all state related to this contract for simpler offchain access
   */
  function loadMeme() public constant returns (uint, uint, address, bytes, bytes) {
    return (
    startPrice,
    saleDuration,
    token,
    imageHash,
    metaHash
    );
  }
}
