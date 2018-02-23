pragma solidity ^0.4.18;

import "./ownership/Ownable.sol";
import "./db/EternalDb.sol";

contract Registry is Ownable {
  event RegistryEntryEvent(address indexed registryEntry, bytes32 indexed eventType, uint version, uint[] data);

  EternalDb public db;
  bool wasConstructed;

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

  modifier onlyRegistryEntryOrFactory() {
    require(isRegistryEntry(msg.sender) || isFactory(msg.sender));
    _;
  }

  modifier notEmergency() {
    require(!isEmergency());
    _;
  }

  function setFactory(address factory, bool _isFactory)
  onlyOwner
  {
    db.setBooleanValue(sha3("isFactory", factory), _isFactory);
  }

  function addRegistryEntry(address _registryEntry)
  onlyFactory
  notEmergency
  {
    db.setBooleanValue(sha3("isRegistryEntry", _registryEntry), true);
  }

  function setEmergency(bool _isEmergency)
  onlyOwner
  {
    db.setBooleanValue(sha3("isEmergency"), _isEmergency);
  }

  function fireRegistryEntryEvent(bytes32 eventType, uint version)
  onlyRegistryEntryOrFactory
  {
    fireRegistryEntryEvent(eventType, version, new uint[](0));
  }

  function fireRegistryEntryEvent(bytes32 eventType, uint version, uint[] data)
  onlyRegistryEntryOrFactory
  {
    RegistryEntryEvent(msg.sender, eventType, version, data);
  }

  function transferEternalDbOwnership(address newOwner)
  onlyOwner
  {
    db.transferOwnership(newOwner);
  }

  function isFactory(address factory) public constant returns (bool) {
    return db.getBooleanValue(sha3("isFactory", factory));
  }

  function isRegistryEntry(address registryEntry) public constant returns (bool) {
    return db.getBooleanValue(sha3("isRegistryEntry", registryEntry));
  }

  function isEmergency() public constant returns (bool) {
    return db.getBooleanValue("isEmergency");
  }
}