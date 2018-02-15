pragma solidity ^0.4.18;

import "Registry.sol";
import "ParameterChange.sol";
import "forwarder/Forwarder.sol";

contract ParameterChangeFactory {
  Registry public registry;
  uint public constant version = 1;

  function ParameterChangeFactory(Registry _registry) {
    registry = _registry;
  }

  function createParameterChange(
    bytes32 _key,
    uint _value
  )
  public
  {
    address paramChange = ParameterChange(new Forwarder());
    ParameterChange(paramChange).construct(
      version,
      msg.sender,
      _key,
      _value
    );
    registry.addRegistryEntry(paramChange);
  }
}