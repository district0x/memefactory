pragma solidity ^0.4.24;

import "./Registry.sol";
import "./proxy/Forwarder.sol";
import "./token/minime/MiniMeToken.sol";

/**
 * @title Base Factory contract for creating RegistryEntry contracts
 *
 * @dev This contract is meant to be extended by other factory contracts
 */

contract RegistryEntryFactory is ApproveAndCallFallBack {
  Registry public registry;
  MiniMeToken public registryToken;

  function RegistryEntryFactory(Registry _registry, MiniMeToken _registryToken) {
    registry = _registry;
    registryToken = _registryToken;
  }

  /**
   * @dev Creates new forwarder contract as registry entry
   * Transfers required deposit from creator into this contract
   * Approves new registry entry address to transfer deposit to itself
   * Adds new registry entry address into the registry

   * @param _creator Creator of registry entry
   * @return Address of a new registry entry forwarder contract
   */
  function createRegistryEntry(address _creator) internal returns (address) {
    uint deposit = registry.db().getUIntValue(registry.depositKey());
    address regEntry = new Forwarder();
    require(registryToken.transferFrom(_creator, this, deposit), "RegistryEntryFactory: couldn't transfer deposit");
    require(registryToken.approve(regEntry, deposit), "RegistryEntryFactory: Deposit not approved");
    registry.addRegistryEntry(regEntry);
    return regEntry;
  }

  /**
   * @dev Function called by MiniMeToken when somebody calls approveAndCall on it.
   * This way token can be transferred to a recipient in a single transaction together with execution
   * of additional logic

   * @param _from Sender of transaction approval
   * @param _amount Amount of approved tokens to transfer
   * @param _token Token that received the approval
   * @param _data Bytecode of a function and passed parameters, that should be called after token approval
   */
  function receiveApproval(
    address _from,
    uint256 _amount,
    address _token,
    bytes _data)
  public
  {
    require(this.call(_data), "RegistryEntryFactory: couldn't call data");
  }
}
