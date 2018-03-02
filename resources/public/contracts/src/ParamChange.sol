pragma solidity ^0.4.18;

import "RegistryEntry.sol";
import "db/EternalDb.sol";

/**
 * @title Contract created for each submitted TCR parameter change.
 *
 * @dev It extends base RegistryEntry with additional state for storing information related to the change.
 * It also contains logic for applying accepted changes.
 * Full copy of this contract is NOT deployed with each submission in order to save gas. Only forwarder contracts
 * pointing into single intance of it.
 */

contract ParamChange is RegistryEntry {

  EternalDb public db;
  string public key;
  EternalDb.Types public valueType;
  uint public value;
  uint public originalValue;
  uint public appliedOn;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders into single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _creator Creator of a meme
   * @param _version Version of Meme contract
   * @param _db EternalDb change will be applied to
   * @param _key Key of a changed parameter
   * @param _value New value of a parameter
   */
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
    originalValue = db.getUIntValue(sha3(key));
  }

  /**
   * @dev Applies the parameter change into EternalDb
   * To be able to make change into a EternalDb, this contract must be given temporary permission to make the change
   * This permission is given by ParamChangeRegistry, which holds permanent permission to make changes into the db
   * Cannot be called when change was already applied
   * Can be called only for whitelisted registry entries
   * Can be called only when original value is still current value
   * Creator gets deposit back
   */
  function applyChange()
  public
  notEmergency
  {
    require(isOriginalValueCurrentValue());
    require(!wasApplied());
    require(isWhitelisted());
    require(registryToken.transfer(creator, deposit));
    db.setUIntValue(sha3(key), value);
    appliedOn = now;
    registry.fireRegistryEntryEvent("changeApplied", version);
  }

  /**
   * @dev Returns whether change was already applied
   */
  function wasApplied() public constant returns (bool) {
    return appliedOn > 0;
  }

  /**
   * @dev Returns whether parameter value at contract creation is still current parameter value
   */
  function isOriginalValueCurrentValue() public constant returns (bool) {
    return db.getUIntValue(sha3(key)) == originalValue;
  }

  /**
   * @dev Returns all state related to this contract for simpler offchain access
   */
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