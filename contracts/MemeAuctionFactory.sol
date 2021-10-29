// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC721/IERC721Receiver.sol";
import "./MemeToken.sol";
import "./proxy/Forwarder.sol";
import "./MemeAuction.sol";

contract MemeAuctionFactory is IERC721Receiver {
  address private dummyTarget; // Keep it here, because this contract is deployed as MutableForwarder

  event MemeAuctionStartedEvent(address indexed memeAuction,
                                uint tokenId,
                                address seller,
                                uint startPrice,
                                uint endPrice,
                                uint duration,
                                string description,
                                uint startedOn);

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
    require(address(_memeToken) != address(0), "MemeAuctionFactory: _memeToken address is 0x0");
    require(!wasConstructed, "MemeAuctionFactory: Was already constructed");

    memeToken = _memeToken;
    wasConstructed = true;
  }

  function onERC721Received(address operator, address _from, uint256 _tokenId, bytes calldata _data) public override returns (bytes4) {
    address payable memeAuction = payable(address(new Forwarder()));
    isMemeAuction[memeAuction] = true;
    MemeAuction(memeAuction).construct(_from, _tokenId);
    memeToken.safeTransferFrom(address(this), memeAuction, _tokenId, _data);
    return IERC721Receiver.onERC721Received.selector;
  }

  function fireMemeAuctionStartedEvent(uint tokenId, address seller, uint startPrice, uint endPrice, uint duration, string memory description, uint startedOn) public
    onlyMemeAuction
  {
    emit MemeAuctionStartedEvent(msg.sender,
                                 tokenId,
                                 seller,
                                 startPrice,
                                 endPrice,
                                 duration,
                                 description,
                                 startedOn);
  }

  function fireMemeAuctionBuyEvent(address buyer, uint price, uint auctioneerCut, uint sellerProceeds) public
    onlyMemeAuction
  {
    emit MemeAuctionBuyEvent(msg.sender, buyer, price, auctioneerCut, sellerProceeds);
  }

  function fireMemeAuctionCanceledEvent() public
    onlyMemeAuction
  {
    emit MemeAuctionCanceledEvent(msg.sender);
  }

}
