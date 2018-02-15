pragma solidity ^0.4.18;

import "token/MintableToken.sol";

contract RegistryEntryToken is MintableToken {
  string public name;
  uint8 public decimals = 0;

  function construct(
    string _name
  ) {
    require(bytes(name).length == 0);
    require(bytes(_name).length != 0);
    name = _name;
  }

  function symbol() public constant returns (string) {
    return name;
  }
}