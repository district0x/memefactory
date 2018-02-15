pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "MemeToken.sol";

contract Meme is RegistryEntry {

  bytes32 public constant applicationPeriodDurationKey = sha3("memeApplicationPeriodDuration");
  bytes32 public constant commitPeriodDurationKey = sha3("memeCommitPeriodDuration");
  bytes32 public constant revealPeriodDurationKey = sha3("memeRevealPeriodDuration");
  bytes32 public constant depositKey = sha3("memeDeposit");
  bytes32 public constant challengeDispensationKey = sha3("memeChallengeDispensation");

  bytes32 public constant maxStartPriceKey = sha3("memeMaxStartPrice");
  bytes32 public constant saleDurationKey = sha3("memeSaleDuration");

  struct Sale {
    uint startPrice;
    uint64 duration;
  }

  struct Meta {
    MemeToken token;
    bytes imageHash;
    bytes metaHash;
  }

  Sale public sale;
  Meta public meta;

  function construct(
    uint _version,
    address _creator,
    string _name,
    bytes _imageHash,
    bytes _metaHash,
    uint _totalSupply,
    uint _startPrice
  )
  public
  {
    super.construct(_version, _creator);
    meta.token = MemeToken(new Forwarder());
    meta.token.construct(_name);
    meta.token.mint(this, _totalSupply);
    meta.token.finishMinting();

    meta.imageHash = _imageHash;
    meta.metaHash = _metaHash;

    require(_startPrice <= parametrizer.getUIntValue(maxStartPriceKey));
    sale = Sale(_startPrice, uint64(parametrizer.getUIntValue(saleDurationKey)));
  }

  function buy(uint _amount)
  payable
  public
  notEmergency
  onlyConstructed
  {
    require(isWhitelisted());
    require(_amount > 0);

    var price = currentPrice() * _amount;

    require(msg.value >= price);
    require(meta.token.transfer(msg.sender, _amount));
    entry.creator.transfer(msg.value);
    if (msg.value > price) {
      msg.sender.transfer(msg.value - price);
    }
    registry.fireRegistryEntryEvent("buy", version);
  }

  function currentPrice() constant returns (uint) {
    uint secondsPassed = 0;
    uint listedOn = whitelistedOn();

    if (now > listedOn && listedOn > 0) {
      secondsPassed = now - listedOn;
    }

    return computeCurrentPrice(
      sale.startPrice,
      sale.duration,
      secondsPassed
    );
  }

  function computeCurrentPrice(uint _startPrice, uint _duration, uint _secondsPassed) constant returns (uint) {
    if (_secondsPassed >= _duration) {
      // We've reached the end of the dynamic pricing portion
      // of the auction, just return the end price.
      return 0;
    } else {
      // Starting price can be higher than ending price (and often is!), so
      // this delta can be negative.
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
}