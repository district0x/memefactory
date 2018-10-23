pragma solidity ^0.4.24;

import "./token/ERC721Receiver.sol";
import "./MemeToken.sol";
import "./proxy/Forwarder.sol";
import "./MemeAuction.sol";

contract MemeAuctionFactory is ERC721Receiver {
  address public target; // Keep it here, because this contract is deployed as MutableForwarder

  event MemeAuctionStartedEvent(address indexed memeAuction,
                                uint tokenId,
                                address seller,
                                uint startPrice,
                                uint endPrice,
                                uint duration);

  event MemeAuctionBuyEvent(address indexed memeAuction,
                            address buyer,
                            uint price,
                            uint auctioneerCut,
                            uint sellerProceeds);

  event MemeAuctionCanceledEvent(address indexed memeAuction);


  MemeToken public memeToken;
  bool public wasConstructed;
  mapping(address => bool) public isMemeAuction;

  modifier onlyMemeAuction() {
    require(isMemeAuction[msg.sender], "MemeAuctionFactory: onlyMemeAuction falied");
    _;
  }

  function construct(MemeToken _memeToken) public {
    require(address(_memeToken) != 0x0, "MemeAuctionFactory: _memeToken address is 0x0");
    require(!wasConstructed, "MemeAuctionFactory: Was already constructed");

    memeToken = _memeToken;
    wasConstructed = true;
  }

  function onERC721Received(address _from, uint256 _tokenId, bytes _data) public returns (bytes4) {
    address memeAuction = new Forwarder();
    isMemeAuction[memeAuction] = true;
    MemeAuction(memeAuction).construct(_from, _tokenId);
    memeToken.safeTransferFrom(address(this), memeAuction, _tokenId, _data);
    return ERC721_RECEIVED;
  }

  function fireMemeAuctionStartedEvent(uint tokenId, address seller, uint startPrice, uint endPrice, uint duration)
    onlyMemeAuction
  {
    emit MemeAuctionStartedEvent(msg.sender,  tokenId,  seller,  startPrice, endPrice, duration);
  }

  function fireMemeAuctionBuyEvent(address buyer, uint price, uint auctioneerCut, uint sellerProceeds)
    onlyMemeAuction
  {
    emit MemeAuctionBuyEvent(msg.sender, buyer, price, auctioneerCut, sellerProceeds);
  }

  function fireMemeAuctionCanceledEvent()
    onlyMemeAuction
  {
    emit MemeAuctionCanceledEvent(msg.sender);
  }

}
