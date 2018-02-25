pragma solidity ^0.4.18;

import "../auth/DSAuth.sol";

/**
 * @title Contract to store arbitrary state data, decoupled from any logic related to it
 *
 * @dev Original implementation: https://blog.colony.io/writing-upgradeable-contracts-in-solidity-6743f0eecc88
 * In addition to original implementation, this contract uses DSAuth for more advanced authentication options
 * It also provides way set/get multiple values in single transaction
 */

contract EternalDb is DSAuth {

  enum Types {UInt, String, Address, Bytes, Bytes32, Boolean, Int}

  function EternalDb(){
  }

  ////////////
  //UInt
  ////////////

  mapping(bytes32 => uint) UIntStorage;

  function getUIntValue(bytes32 record) constant returns (uint){
    return UIntStorage[record];
  }

  function getUIntValues(bytes32[] records) constant returns (uint[] results){
    results = new uint[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = UIntStorage[records[i]];
    }
  }

  function setUIntValue(bytes32 record, uint value)
  auth
  {
    UIntStorage[record] = value;
  }

  function setUIntValues(bytes32[] records, uint[] values)
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      UIntStorage[records[i]] = values[i];
    }
  }

  function deleteUIntValue(bytes32 record)
  auth
  {
    delete UIntStorage[record];
  }

  ////////////
  //Strings
  ////////////

  mapping(bytes32 => string) StringStorage;

  function getStringValue(bytes32 record) constant returns (string){
    return StringStorage[record];
  }

  function setStringValue(bytes32 record, string value)
  auth
  {
    StringStorage[record] = value;
  }

  function deleteStringValue(bytes32 record)
  auth
  {
    delete StringStorage[record];
  }

  ////////////
  //Address
  ////////////

  mapping(bytes32 => address) AddressStorage;

  function getAddressValue(bytes32 record) constant returns (address){
    return AddressStorage[record];
  }

  function setAddressValues(bytes32[] records, address[] values)
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      AddressStorage[records[i]] = values[i];
    }
  }

  function setAddressValue(bytes32 record, address value)
  auth
  {
    AddressStorage[record] = value;
  }

  function deleteAddressValue(bytes32 record)
  auth
  {
    delete AddressStorage[record];
  }

  ////////////
  //Bytes
  ////////////

  mapping(bytes32 => bytes) BytesStorage;

  function getBytesValue(bytes32 record) constant returns (bytes){
    return BytesStorage[record];
  }

  function setBytesValue(bytes32 record, bytes value)
  auth
  {
    BytesStorage[record] = value;
  }

  function deleteBytesValue(bytes32 record)
  auth
  {
    delete BytesStorage[record];
  }

  ////////////
  //Bytes32
  ////////////

  mapping(bytes32 => bytes32) Bytes32Storage;

  function getBytes32Value(bytes32 record) constant returns (bytes32){
    return Bytes32Storage[record];
  }

  function getBytes32Values(bytes32[] records) constant returns (bytes32[] results){
    results = new bytes32[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = Bytes32Storage[records[i]];
    }
  }

  function setBytes32Value(bytes32 record, bytes32 value)
  auth
  {
    Bytes32Storage[record] = value;
  }

  function setBytes32Values(bytes32[] records, bytes32[] values)
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      Bytes32Storage[records[i]] = values[i];
    }
  }

  function deleteBytes32Value(bytes32 record)
  auth
  {
    delete Bytes32Storage[record];
  }

  ////////////
  //Boolean
  ////////////

  mapping(bytes32 => bool) BooleanStorage;

  function getBooleanValue(bytes32 record) constant returns (bool){
    return BooleanStorage[record];
  }

  function getBooleanValues(bytes32[] records) constant returns (bool[] results){
    results = new bool[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = BooleanStorage[records[i]];
    }
  }

  function setBooleanValue(bytes32 record, bool value)
  auth
  {
    BooleanStorage[record] = value;
  }

  function setBooleanValues(bytes32[] records, bool[] values)
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      BooleanStorage[records[i]] = values[i];
    }
  }

  function deleteBooleanValue(bytes32 record)
  auth
  {
    delete BooleanStorage[record];
  }

  ////////////
  //Int
  ////////////
  mapping(bytes32 => int) IntStorage;

  function getIntValue(bytes32 record) constant returns (int){
    return IntStorage[record];
  }

  function getIntValues(bytes32[] records) constant returns (int[] results){
    results = new int[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = IntStorage[records[i]];
    }
  }

  function setIntValue(bytes32 record, int value)
  auth
  {
    IntStorage[record] = value;
  }

  function setIntValues(bytes32[] records, int[] values)
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      IntStorage[records[i]] = values[i];
    }
  }

  function deleteIntValue(bytes32 record)
  auth
  {
    delete IntStorage[record];
  }

}
