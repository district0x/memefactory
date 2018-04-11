pragma solidity ^0.4.18;

import "auth/DSAuth.sol";

contract DistrictConfig is DSAuth {
  address public memeAuctionCutCollector;
  uint public memeAuctionCut; // Values 0-10,000 map to 0%-100%

  function DistrictConfig(address _memeAuctionCutCollector, uint _memeAuctionCut) {
    require(_memeAuctionCutCollector != 0x0);
    require(_memeAuctionCut < 10000);
    memeAuctionCutCollector = _memeAuctionCutCollector;
    memeAuctionCut = _memeAuctionCut;
  }

  function setMemeAuctionCutCollector(address _memeAuctionCutCollector) public auth {
    require(_memeAuctionCutCollector != 0x0);
    memeAuctionCutCollector = _memeAuctionCutCollector;
  }

  function setMemeAuctionCut(uint _memeAuctionCut) public auth {
    require(_memeAuctionCut < 10000);
    memeAuctionCut = _memeAuctionCut;
  }
}
