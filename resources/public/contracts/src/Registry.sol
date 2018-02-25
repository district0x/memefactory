pragma solidity ^0.4.18;

import "auth/DSAuth.sol";
import "db/EternalDb.sol";

/**
 * @title Central contract for TCR registry
 *
 * @dev Manages state about deployed registry entries and factories
 * Serves as a central point for firing all registry entry events
 * This contract is not accessed directly, but through MutableForwarder. See MutableForwarder.sol for more comments.
 */

contract Registry is DSAuth {
  event RegistryEntryEvent(address indexed registryEntry, bytes32 indexed eventType, uint version, uint[] data);

  EternalDb public db;
  bool wasConstructed;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because we use a forwarder pointing to single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _db Address of EternalDb related to this registry
   */
  function construct(EternalDb _db) {
    require(!wasConstructed);
    require(address(_db) != 0x0);
    db = _db;
    wasConstructed = true;
    owner = msg.sender;
  }

  modifier onlyFactory() {
    require(isFactory(msg.sender));
    _;
  }

  modifier onlyRegistryEntry() {
    require(isRegistryEntry(msg.sender));
    _;
  }

  modifier notEmergency() {
    require(!isEmergency());
    _;
  }

  /**
   * @dev Sets whether address is factory allowed to add registry entries into registry
   * Must be callable only by authenticated user

   * @param _factory Address of a factory contract
   * @param _isFactory Whether the address is allowed factory
   */
  function setFactory(address _factory, bool _isFactory)
  auth
  {
    db.setBooleanValue(sha3("isFactory", _factory), _isFactory);
  }

  /**
   * @dev Adds address as valid registry entry into the Registry
   * Must be callable only by allowed factory contract

   * @param _registryEntry Address of new registry entry
   */
  function addRegistryEntry(address _registryEntry)
  onlyFactory
  notEmergency
  {
    db.setBooleanValue(sha3("isRegistryEntry", _registryEntry), true);
  }

  /**
   * @dev Sets emergency state to pause all trading operations
   * Must be callable only by authenticated user

   * @param _isEmergency True if emergency is happening
   */
  function setEmergency(bool _isEmergency)
  auth
  {
    db.setBooleanValue(sha3("isEmergency"), _isEmergency);
  }

  function fireRegistryEntryEvent(bytes32 _eventType, uint _version)
  onlyRegistryEntry
  {
    fireRegistryEntryEvent(_eventType, _version, new uint[](0));
  }

  /**
   * @dev Fires event related to a registry entry
   * Must be callable only by valid registry entry

   * @param _eventType String identifying event type
   * @param _version Version of registry entry contract
   * @param _data Additional data related to event
   */
  function fireRegistryEntryEvent(bytes32 _eventType, uint _version, uint[] _data)
  onlyRegistryEntry
  {
    RegistryEntryEvent(msg.sender, _eventType, _version, _data);
  }

  /**
   * @dev Returns whether address is valid registry entry factory

   * @return True if address is factory
   */
  function isFactory(address factory) public constant returns (bool) {
    return db.getBooleanValue(sha3("isFactory", factory));
  }

  /**
   * @dev Returns whether address is valid registry entry

   * @return True if address is registry entry
   */
  function isRegistryEntry(address registryEntry) public constant returns (bool) {
    return db.getBooleanValue(sha3("isRegistryEntry", registryEntry));
  }

  /**
   * @dev Returns whether emergency stop is happening

   * @return True if emergency is happening
   */
  function isEmergency() public constant returns (bool) {
    return db.getBooleanValue("isEmergency");
  }
}