pragma solidity ^0.4.18;

import "../ownership/Ownable.sol";

contract EternalStorage is Ownable {

  enum Types {UInt, String, Address, Bytes, Bytes32, Boolean, Int}

  function EternalStorage(){
  }

  ////////////
  //UInt
  ////////////

  mapping(bytes32 => uint) UIntStorage;

  function getUIntValue(bytes32 record) constant returns (uint){
    return UIntStorage[record];
  }

  function setUIntValue(bytes32 record, uint value)
  onlyOwner
  {
    UIntStorage[record] = value;
  }

  function setUIntValues(bytes32[] records, uint[] values)
  onlyOwner
  {
    for (uint i = 0; i < records.length; i++) {
      UIntStorage[records[i]] = values[i];
    }
  }

  function deleteUIntValue(bytes32 record)
  onlyOwner
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
  onlyOwner
  {
    StringStorage[record] = value;
  }

  function deleteStringValue(bytes32 record)
  onlyOwner
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
  onlyOwner
  {
    for (uint i = 0; i < records.length; i++) {
      AddressStorage[records[i]] = values[i];
    }
  }

  function setAddressValue(bytes32 record, address value)
  onlyOwner
  {
    AddressStorage[record] = value;
  }

  function deleteAddressValue(bytes32 record)
  onlyOwner
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
  onlyOwner
  {
    BytesStorage[record] = value;
  }

  //  function setBytesValues(bytes32[] records, bytes[] values)
  //  onlyOwner
  //  {
  //    for (uint i = 0; i < records.length; i++) {
  //      BytesStorage[records[i]] = values[i];
  //    }
  //  }

  function deleteBytesValue(bytes32 record)
  onlyOwner
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

  function setBytes32Value(bytes32 record, bytes32 value)
  onlyOwner
  {
    Bytes32Storage[record] = value;
  }

  function setBytes32Values(bytes32[] records, bytes32[] values)
  onlyOwner
  {
    for (uint i = 0; i < records.length; i++) {
      Bytes32Storage[records[i]] = values[i];
    }
  }

  function deleteBytes32Value(bytes32 record)
  onlyOwner
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

  function setBooleanValue(bytes32 record, bool value)
  onlyOwner
  {
    BooleanStorage[record] = value;
  }

  function setBooleanValues(bytes32[] records, bool[] values)
  onlyOwner
  {
    for (uint i = 0; i < records.length; i++) {
      BooleanStorage[records[i]] = values[i];
    }
  }

  function deleteBooleanValue(bytes32 record)
  onlyOwner
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

  function setIntValue(bytes32 record, int value)
  onlyOwner
  {
    IntStorage[record] = value;
  }

  function setIntValues(bytes32[] records, int[] values)
  onlyOwner
  {
    for (uint i = 0; i < records.length; i++) {
      IntStorage[records[i]] = values[i];
    }
  }

  function deleteIntValue(bytes32 record)
  onlyOwner
  {
    delete IntStorage[record];
  }

}
