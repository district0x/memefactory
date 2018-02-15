pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "./storage/EternalStorage.sol";

contract ParameterChange is RegistryEntry {

  bytes32 public constant applicationPeriodDurationKey = sha3("paramApplicationPeriodDuration");
  bytes32 public constant commitPeriodDurationKey = sha3("paramCommitPeriodDuration");
  bytes32 public constant revealPeriodDurationKey = sha3("paramRevealPeriodDuration");
  bytes32 public constant depositKey = sha3("paramDeposit");
  bytes32 public constant challengeDispensationKey = sha3("paramChallengeDispensation");

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