pragma solidity ^0.4.18;

import "RegistryEntryFactory.sol";
import "ParamChange.sol";

contract ParamChangeFactory is RegistryEntryFactory {
  uint public constant version = 1;

  function ParamChangeFactory(Registry _registry, MiniMeToken _registryToken)
  RegistryEntryFactory(_registry, _registryToken)
  {}

  function createParamChange(
    address _creator,
    address _db,
    string _key,
    uint _value
  )
  public
  {
    ParamChange paramChange = ParamChange(createRegistryEntry(_creator));

    paramChange.construct(
      _creator,
      version,
      _db,
      _key,
      _value
    );
  }
}