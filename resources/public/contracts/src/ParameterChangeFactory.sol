pragma solidity ^0.4.18;

import "ParameterRegistry.sol";
import "ParameterChange.sol";
import "forwarder/Forwarder.sol";

contract ParameterChangeFactory {
  ParameterRegistry public registry;
  uint public constant version = 1;

  function ParameterChangeFactory(ParameterRegistry _registry) {
    registry = _registry;
  }

  function createParameterChange(
    address _creator,
    bytes32 _key,
    uint _value
  )
  public
  {
    address paramChange = ParameterChange(new Forwarder());
    registry.addRegistryEntry(paramChange);

    ParameterChange(paramChange).construct(
      _creator,
      version,
      _key,
      _value
    );
  }
}