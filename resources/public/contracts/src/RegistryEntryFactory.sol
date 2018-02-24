pragma solidity ^0.4.18;

import "Registry.sol";
import "forwarder/Forwarder.sol";
import "token/minime/MiniMeToken.sol";

contract RegistryEntryFactory is ApproveAndCallFallBack {
  Registry public registry;
  MiniMeToken public registryToken;
  bytes32 public constant depositKey = sha3("deposit");

  function RegistryEntryFactory(Registry _registry, MiniMeToken _registryToken) {
    registry = _registry;
    registryToken = _registryToken;
  }

  function createRegistryEntry(address _creator) internal returns (address) {
    uint deposit = registry.db().getUIntValue(depositKey);
    address regEntry = new Forwarder();
    require(registryToken.transferFrom(_creator, this, deposit));
    require(registryToken.approve(regEntry, deposit));
    registry.addRegistryEntry(regEntry);
    return regEntry;
  }

  function receiveApproval(
    address from,
    uint256 _amount,
    address _token,
    bytes _data)
  public
  {
    require(this.call(_data));
  }
}