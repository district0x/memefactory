pragma solidity ^0.4.24;

import "./RegistryEntryFactory.sol";
import "./ParamChange.sol";

/**
 * @title Factory contract for creating ParamChange contracts
 *
 * @dev Users submit new TCR parameter changes into this contract.
 */

contract ParamChangeFactory is RegistryEntryFactory {
  uint public constant version = 1;

  function ParamChangeFactory(Registry _registry, MiniMeToken _registryToken)
  RegistryEntryFactory(_registry, _registryToken)
  {}

  /**
   * @dev Creates new ParamChange forwarder contract and add it into the registry
   * It initializes forwarder contract with initial state. For comments on each param, see ParamChange::construct
   */
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
