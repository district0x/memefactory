pragma solidity ^0.4.18;

import "./Meme.sol";
import "./MemeSale.sol";
import "./Forwarder.sol";
import "./Registry.sol";
import "./ownership/Ownable.sol";

contract MemeFactory is Ownable {
  Registry public registry;
  MemeSale public memeSale;

  function MemeFactory(Registry _registry, MemeSale _memeSale) {
    registry = _registry;
    memeSale = _memeSale;
  }

  function submitMeme(
    string _name,
    bytes _image,
    bytes _meta,
    uint _totalSupply,
    uint _startPrice
  ) {
    var forwarder = address(new Forwarder());
    require(_totalSupply >= 1);
    require(_totalSupply <= registry.parameterizer().get("maxTotalSupply"));
    require(_startPrice <= registry.parameterizer().get("maxStartPrice"));
    Meme(forwarder).construct(
      _name,
      _image,
      _meta,
      _totalSupply,
      _startPrice,
      uint64(registry.parameterizer().get("saleDuration")),
      memeSale
    );
    registry.apply(forwarder);
  }

  function setRegistry(Registry _registry) onlyOwner {
    registry = _registry;
  }

  function setMemeSale(MemeSale _memeSale) onlyOwner {
    memeSale = _memeSale;
  }
}
