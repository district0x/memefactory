// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "../proxy/DelegateProxy.sol";

contract Forwarder is DelegateProxy {
  // After compiling contract, `beefbeef...` is replaced in the bytecode by the real target address
  address public constant target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksumed to silence warning

  /*
  * @dev Forwards all calls to target
  */
  fallback () external payable {
    delegatedFwd(target, msg.data);
  }
}