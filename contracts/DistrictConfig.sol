pragma solidity ^0.4.24;

import "./auth/DSAuth.sol";

contract DistrictConfig is DSAuth {
  address public depositCollector;
  address public memeAuctionCutCollector;
  uint public memeAuctionCut; // Values 0-10,000 map to 0%-100%

  function DistrictConfig(address _depositCollector, address _memeAuctionCutCollector, uint _memeAuctionCut) {
    require(_depositCollector != 0x0, "District Config deposit collector isn't 0x0");
    require(_memeAuctionCutCollector != 0x0, "District Config meme auction cut collector isn't 0x0");
    require(_memeAuctionCut < 10000, "District Config meme auction cut should be < 1000");
    depositCollector = _depositCollector;
    memeAuctionCutCollector = _memeAuctionCutCollector;
    memeAuctionCut = _memeAuctionCut;
  }

  function setDepositCollector(address _depositCollector) public auth {
    require(_depositCollector != 0x0, "District Config deposit collector isn't 0x0");
    depositCollector = _depositCollector;
  }

  function setMemeAuctionCutCollector(address _memeAuctionCutCollector) public auth {
    require(_memeAuctionCutCollector != 0x0, "District Config meme auction cut collector isn't 0x0");
    memeAuctionCutCollector = _memeAuctionCutCollector;
  }

  function setCollectors(address _collector) public auth {
    setDepositCollector(_collector);
    setMemeAuctionCutCollector(_collector);
  }

  function setMemeAuctionCut(uint _memeAuctionCut) public auth {
    require(_memeAuctionCut < 10000, "District Config meme auction cut should be < 1000");
    memeAuctionCut = _memeAuctionCut;
  }
}
