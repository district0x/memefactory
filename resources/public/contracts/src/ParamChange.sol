pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "db/EternalDb.sol";

contract ParamChange is RegistryEntry {

  EternalDb public db;
  string public key;
  EternalDb.Types public valueType;
  uint public value;
  uint public appliedOn;

  function construct(
    address _creator,
    uint _version,
    address _db,
    string _key,
    uint _value
  )
  public
  {
    super.construct(_creator, _version);
    db = EternalDb(_db);
    key = _key;
    value = _value;
    valueType = EternalDb.Types.UInt;
  }

  function applyChange() public {
    require(!wasApplied());
    require(isWhitelisted());
    db.setUIntValue(sha3(key), value);
    appliedOn = now;
  }

  function wasApplied() public constant returns (bool) {
    return appliedOn > 0;
  }

  function loadParamChange() public constant returns (EternalDb, string, EternalDb.Types, uint, uint) {
    return (
    db,
    key,
    valueType,
    value,
    appliedOn
    );
  }
}