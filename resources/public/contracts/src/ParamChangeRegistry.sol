pragma solidity ^0.4.18;

import "Registry.sol";
import "ParamChange.sol";
import "auth/DSGuard.sol";

contract ParamChangeRegistry is Registry {

  function addRegistryEntry(address _registryEntry) {
    super.addRegistryEntry(_registryEntry);
    address lastEntry = db.getAddressValue(sha3("lastRegistryEntry"));
    if (lastEntry != 0x0) {
      db.setAddressValue(sha3("previousRegistryEntry", _registryEntry), lastEntry);
    }
    db.setAddressValue(sha3("lastRegistryEntry"), _registryEntry);
  }

  function applyParamChange(ParamChange _paramChange) {
    require(isRegistryEntry(_paramChange));
    ParamChange prevParamChange = ParamChange(db.getAddressValue(sha3("previousRegistryEntry", _paramChange)));
    require(address(prevParamChange) == 0x0 || prevParamChange.wasApplied() || prevParamChange.isBlacklisted());
    DSGuard guard = DSGuard(_paramChange.db().authority());
    guard.permit(_paramChange, _paramChange.db(), guard.ANY());
    prevParamChange.applyChange();
    guard.forbid(_paramChange, _paramChange.db(), guard.ANY());
  }
}

