pragma solidity ^0.4.18;

import "forwarder/Forwarder.sol";
import "auth/DSAuth.sol";

contract MutableForwarder is Forwarder, DSAuth {

  address target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksumed to silence warning

  function replaceTarget(address _target) public auth {
    target = _target;
  }

}