pragma solidity ^0.4.18;

import "./Registry.sol";
import "./Meme.sol";
import "./ownership/Ownable.sol";

contract MemeSale is Ownable {
  Registry public registry;

  function MemeSale(Registry _registry) {
    registry = _registry;
  }

  function setRegistry(Registry _registry) onlyOwner {
    registry = _registry;
  }

  function buy(address listingHash, uint _amount) payable {
    bool whitelisted = false;
    address owner;
    (, whitelisted, owner,,,) = registry.listings(listingHash);
    require(whitelisted);
    require(_amount > 0);

    Meme meme = Meme(listingHash);
    var price = currentPrice(listingHash) * _amount;

    require(msg.value >= price);
    require(meme.transfer(msg.sender, _amount));
    owner.transfer(msg.value);
    if (msg.value > price) {
      msg.sender.transfer(msg.value - price);
    }
  }

  function currentPrice(address listingHash)
    constant
    returns (uint256)
  {
    uint256 secondsPassed = 0;
    uint64 whitelistedOn = 0;
    Meme meme = Meme(listingHash);
    (,,,,, whitelistedOn) = registry.listings(listingHash);

    if (now > whitelistedOn && whitelistedOn > 0) {
      secondsPassed = now - whitelistedOn;
    }

    return computeCurrentPrice(
      meme.startPrice(),
      meme.saleDuration(),
      secondsPassed
    );
  }

  function computeCurrentPrice(
    uint256 _startPrice,
    uint256 _duration,
    uint256 _secondsPassed
  )
    constant
    returns (uint256)
  {
    if (_secondsPassed >= _duration) {
      // We've reached the end of the dynamic pricing portion
      // of the auction, just return the end price.
      return 0;
    } else {
      // Starting price can be higher than ending price (and often is!), so
      // this delta can be negative.
      int256 totalPriceChange = 0 - int256(_startPrice);

      // This multiplication can't overflow, _secondsPassed will easily fit within
      // 64-bits, and totalPriceChange will easily fit within 128-bits, their product
      // will always fit within 256-bits.
      int256 currentPriceChange = totalPriceChange * int256(_secondsPassed) / int256(_duration);

      // currentPriceChange can be negative, but if so, will have a magnitude
      // less that _startingPrice. Thus, this result will always end up positive.
      int256 currentPrice = int256(_startPrice) + currentPriceChange;

      return uint256(currentPrice);
    }
  }
}
