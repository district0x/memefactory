pragma solidity ^0.4.21;

import "./token/ERC721Receiver.sol";
import "./MemeToken.sol";
import "./Registry.sol";
import "./Meme.sol";
import "math/SafeMath.sol";
import "./MemeAuctionFactory.sol";
import "./DistrictConfig.sol";

contract MemeAuction is ERC721Receiver {
  using SafeMath for uint;

  bytes32 public constant maxDurationKey = sha3("maxAuctionDuration");
  DistrictConfig public constant districtConfig = DistrictConfig(0xABCDabcdABcDabcDaBCDAbcdABcdAbCdABcDABCd);
  Registry public constant registry = Registry(0xfEEDFEEDfeEDFEedFEEdFEEDFeEdfEEdFeEdFEEd);
  MemeToken public constant memeToken = MemeToken(0xdaBBdABbDABbDabbDaBbDabbDaBbdaBbdaBbDAbB);
  MemeAuctionFactory public constant memeAuctionFactory = MemeAuctionFactory(0xdAFfDaFfDAfFDAFFDafFdAfFdAffdAffDAFFdAFF);
  address public seller;
  uint public tokenId;
  uint public startPrice;
  uint public endPrice;
  uint public duration;
  uint public startedOn;

  modifier notEmergency() {
    require(!registry.isEmergency());
    _;
  }

  function construct(address _seller, uint _tokenId)
  notEmergency
  {
    require(_seller != 0x0);
    require(seller == 0x0);
    seller = _seller;
    tokenId = _tokenId;
  }

  function startAuction(uint _startPrice, uint _endPrice, uint _duration)
  notEmergency
  {
    require(memeToken.ownerOf(tokenId) == address(this));
    require(startedOn == 0);
    require(_duration <= registry.db().getUIntValue(maxDurationKey));
//    // Require that all auctions have a duration of
//    // at least one minute. (Keeps our math from getting hairy!)
    require(_duration >= 1 minutes);
    startedOn = now;
    memeAuctionFactory.fireMemeAuctionEvent("auctionStarted");
  }

  /**
   * @dev Buys meme from auction
   * Seller gets ETH paid for a meme token
   * If buyer sends more than is current price, extra ETH is sent back to the buyer

   */
  function buy()
  payable
  public
  notEmergency
  {
    require(startedOn > 0);
    var price = currentPrice();
    require(msg.value >= price);
    uint auctioneerCut = 0;
    uint sellerProceeds = 0;
    if (price > 0) {
      auctioneerCut = computeCut(price);
      sellerProceeds = price.sub(auctioneerCut);

      seller.transfer(sellerProceeds);
      if (msg.value > price) {
        msg.sender.transfer(msg.value.sub(price));
      }
      if (auctioneerCut > 0) {
        districtConfig.memeAuctionCutCollector().transfer(auctioneerCut);
      }
    }
    memeToken.safeTransferFrom(this, msg.sender, tokenId);

    var eventData = new uint[](4);
    eventData[0] = uint(msg.sender);
    eventData[1] = price;
    eventData[2] = auctioneerCut;
    eventData[3] = sellerProceeds;
    memeAuctionFactory.fireMemeAuctionEvent("buy", eventData);
  }

  function cancel() public {
    require(startedOn > 0);
    require(msg.sender == seller);
    memeToken.safeTransferFrom(this, seller, tokenId);
    memeAuctionFactory.fireMemeAuctionEvent("canceled");
  }

  function onERC721Received(address _from, uint256 _tokenId, bytes _data)
  public
  notEmergency
  returns (bytes4)
  {
    require(_tokenId == tokenId);
    require(startedOn == 0);
    require(this.call(_data));
    return ERC721_RECEIVED;
  }

  /// @dev Returns current price of an NFT on auction. Broken into two
  ///  functions (this one, that computes the duration from the auction
  ///  structure, and the other that does the price computation) so we
  ///  can easily test that the price computation works correctly.
  function currentPrice()
  public
  constant
  returns (uint256)
  {
    uint256 secondsPassed = 0;

    // A bit of insurance against negative values (or wraparound).
    // Probably not necessary (since Ethereum guarnatees that the
    // now variable doesn't ever go backwards).
    if (now > startedOn) {
      secondsPassed = now - startedOn;
    }

    return _computeCurrentPrice(
      startPrice,
      endPrice,
      duration,
      secondsPassed
    );
  }


  /// @dev Computes the current price of an auction. Factored out
  ///  from _currentPrice so we can run extensive unit tests.
  ///  When testing, make this function public and turn on
  ///  `Current price computation` test suite.
  function _computeCurrentPrice(
    uint256 _startingPrice,
    uint256 _endingPrice,
    uint256 _duration,
    uint256 _secondsPassed
  )
  internal
  pure
  returns (uint256)
  {
    // NOTE: We don't use SafeMath (or similar) in this function because
    //  all of our public functions carefully cap the maximum values for
    //  time (at 64-bits) and currency (at 128-bits). _duration is
    //  also known to be non-zero (see the require() statement in
    //  _addAuction())
    if (_secondsPassed >= _duration) {
      // We've reached the end of the dynamic pricing portion
      // of the auction, just return the end price.
      return _endingPrice;
    } else {
      // Starting price can be higher than ending price (and often is!), so
      // this delta can be negative.
      int256 totalPriceChange = int256(_endingPrice) - int256(_startingPrice);

      // This multiplication can't overflow, _secondsPassed will easily fit within
      // 64-bits, and totalPriceChange will easily fit within 128-bits, their product
      // will always fit within 256-bits.
      int256 currentPriceChange = totalPriceChange * int256(_secondsPassed) / int256(_duration);

      // currentPriceChange can be negative, but if so, will have a magnitude
      // less that _startingPrice. Thus, this result will always end up positive.
      int256 currentPrice = int256(_startingPrice) + currentPriceChange;

      return uint256(currentPrice);
    }
  }

  /// @dev Computes owner's cut of a sale.
  /// @param _price - Sale price of NFT.
  function computeCut(uint256 _price) public constant returns (uint256) {
    return _price.add(districtConfig.memeAuctionCut()) / 10000;
  }

  /**
   * @dev Returns all state related to this contract for simpler offchain access
   */
  function loadMemeAuction() public constant returns (address, uint, uint, uint, uint, uint, address) {
    return (
    seller,
    tokenId,
    startPrice,
    endPrice,
    duration,
    startedOn,
    memeToken.tokenURI(tokenId)
    );
  }

  function() public payable {
    buy();
  }
}