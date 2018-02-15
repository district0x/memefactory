pragma solidity ^0.4.18;

contract MutableForwarder is Forwarder, Ownable {

  address target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef;

  function replaceTarget(address _target) public onlyOwner {
    target = _target;
  }

}