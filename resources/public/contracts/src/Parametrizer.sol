pragma solidity ^0.4.18;

import "./ownership/Ownable.sol";
import "./storage/EternalStorage.sol";

contract Parametrizer is Ownable {
  event ParametrizerChangeEvent(address indexed parametrizerChange, bytes32 indexed eventType, uint version, uint[] data);

  EternalStorage public eternalStorage;

  function Registry(address _eternalStorage) {
    if (_eternalStorage == 0x0) {
      eternalStorage = new EternalStorage();
    } else {
      eternalStorage = EternalStorage(_eternalStorage);
    }
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

  function setFactory(address factory, bool _isFactory)
  onlyOwner
  {
    eternalStorage.setBooleanValue(sha3("isFactory", factory), _isFactory);
  }

  function addRegistryEntry(address _registryEntry)
  onlyFactory
  notEmergency
  {
    eternalStorage.setBooleanValue(sha3("isRegistryEntry", _registryEntry), true);
  }

  function setEmergency(bool _isEmergency)
  onlyOwner
  {
    eternalStorage.setBooleanValue(sha3("isEmergency"), _isEmergency);
  }

  function fireRegistryEntryEvent(bytes32 eventType, uint version)
  {
    fireRegistryEntryEvent(eventType, version, new uint[](0));
  }

  function fireRegistryEntryEvent(bytes32 eventType, uint version, uint[] data)
  onlyRegistryEntry
  {
    RegistryEntryEvent(msg.sender, eventType, version, data);
  }

  function transferEternalStorageOwnership(address newOwner)
  onlyOwner
  {
    eternalStorage.transferOwnership(newOwner);
  }

  function isFactory(address factory) public constant returns (bool) {
    return eternalStorage.getBooleanValue(sha3("isFactory", factory));
  }

  function isRegistryEntry(address registryEntry) public constant returns (bool) {
    return eternalStorage.getBooleanValue(sha3("isFactory", registryEntry));
  }

  function isEmergency() public constant returns (bool) {
    return eternalStorage.getBooleanValue("isEmergency");
  }
}