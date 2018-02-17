pragma solidity ^0.4.18;

import "./forwarder/Forwarder.sol";
import "./ownership/Ownable.sol";

contract MutableForwarder is Forwarder, Ownable {

  address target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef;

  function replaceTarget(address _target) public onlyOwner {
    target = _target;
  }

}