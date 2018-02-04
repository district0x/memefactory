pragma solidity ^0.4.18;

import "./token/MintableToken.sol";
import "./MemeSale.sol";

contract Meme is MintableToken {
  uint8 public decimals = 0;
  string public name;
  bytes public image;
  bytes public meta;
  uint public startPrice;
  uint64 public saleDuration;
  MemeSale public memeSale;

  function construct(
    string _name,
    bytes _image,
    bytes _meta,
    uint _totalSupply,
    uint _startPrice,
    uint64 _saleDuration,
    MemeSale _memeSale
  )
    canMint
  {
    owner = msg.sender;
    name = _name;
    image = _image;
    meta = _meta;
    memeSale = _memeSale;
    startPrice = _startPrice;
    saleDuration = _saleDuration;
    mint(_memeSale, _totalSupply);
    finishMinting();
  }
}
