pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "./storage/EternalStorage.sol";

contract ParameterChange is RegistryEntry {

  struct Change {
    bytes32 key;
    uint uintValue;
    EternalStorage.Types valueType;
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
    change = new Change(key, uintValue, EternalStorage.Types.UInt);
  }
}