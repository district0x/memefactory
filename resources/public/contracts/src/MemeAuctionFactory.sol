pragma solidity ^0.4.24;

import "./token/ERC721Receiver.sol";
import "./MemeToken.sol";
import "./proxy/Forwarder.sol";
import "./MemeAuction.sol";

contract MemeAuctionFactory is ERC721Receiver {
  address public target; // Keep it here, because this contract is deployed as MutableForwarder

  event MemeAuctionEvent(address indexed memeAuction, bytes32 indexed eventType, uint version, uint timestamp, uint[] data);

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

  function fireMemeAuctionEvent(bytes32 _eventType)
  onlyMemeAuction
  {
    fireMemeAuctionEvent(_eventType, new uint[](0));
  }

  /**
   * @dev Fires event related to a meme auction
   * Must be callable only by valid meme auction

   * @param _eventType String identifying event type
   * @param _data Additional data related to event
   */
  function fireMemeAuctionEvent(bytes32 _eventType, uint[] _data)
  onlyMemeAuction
  {
    MemeAuctionEvent(msg.sender, _eventType, 1, now, _data);
  }
}
