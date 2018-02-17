pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "./storage/EternalStorage.sol";

contract ParameterChange is RegistryEntry {

  struct Change {
    bytes32 key;
    uint uintValue;
    EternalStorage.Types valueType;
    bool wasApplied;
  }

  Change public change;

  function construct(
    uint _version,
    address _creator,
    bytes32 _key,
    uint _uintValue
  )
  public
  {
    super.construct(_version, _creator);
    change = Change(_key, _uintValue, EternalStorage.Types.UInt, false);
  }

  function applyChange(EternalStorage paramStorage) public {
    paramStorage.setUIntValue(change.key, change.uintValue);
    change.wasApplied = true;
    paramStorage.transferOwnership(msg.sender);
  }

  function wasApplied() public constant returns (bool) {
    return change.wasApplied;
  }
}