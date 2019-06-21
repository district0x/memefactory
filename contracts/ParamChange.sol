pragma solidity ^0.4.24;

import "./RegistryEntry.sol";
import "./db/EternalDb.sol";

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
  string private key;
  EternalDb.Types private valueType;
  uint private value;
  uint private originalValue;
  uint private appliedOn;
  bytes private metaHash;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because users create only forwarders into single instance of this contract,
   * therefore constructor must be called explicitly.
   * Can only be called if the parameter value is within its allowed domain.

   * @param _creator Creator of a meme
   * @param _version Version of Meme contract
   * @param _db EternalDb change will be applied to
   * @param _key Key of a changed parameter
   * @param _value New value of a parameter
   * @param _metaHash The ipfs hash of the param change meta
   */
  function construct(
    address _creator,
    uint _version,
    address _db,
    string _key,
    uint _value,
    bytes _metaHash
  )
  external
  {
    bytes32 record = sha3(_key);
    require(RegistryEntryLib.isChangeAllowed(registry, record, _value));

    super.construct(_creator, _version);

    db = EternalDb(_db);
    key = _key;
    value = _value;
    metaHash = _metaHash;
    valueType = EternalDb.Types.UInt;
    originalValue = db.getUIntValue(record);

    registry.fireParamChangeConstructedEvent(version,
                                             _creator,
                                             db,
                                             _key,
                                             value,
                                             deposit,
                                             challenge.challengePeriodEnd,
                                             metaHash);
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
  external
  notEmergency
  {
    require(db.getUIntValue(sha3(key)) == originalValue, "Value is not the same as the original value");
    require(appliedOn <= 0, "Parameter change already applied");
    require(challenge.isWhitelisted(), "Can't apply parameter change, registry entry not whitelisted");
    require(registryToken.transfer(creator, deposit), "Couldn't transfer deposit back to creator");

    db.setUIntValue(sha3(key), value);
    appliedOn = now;

    registry.fireParamChangeAppliedEvent(version);
  }

}
