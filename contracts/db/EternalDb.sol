// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

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

  event EternalDbEvent(bytes32[] records, uint[] values, uint timestamp);

  constructor(){
  }

  ////////////
  //UInt
  ////////////

  mapping(bytes32 => uint) UIntStorage;

  function getUIntValue(bytes32 record) view public returns (uint){
    return UIntStorage[record];
  }

  function getUIntValues(bytes32[] memory records) view public returns (uint[] memory results){
    results = new uint[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = UIntStorage[records[i]];
    }
  }

  function setUIntValue(bytes32 record, uint value) public
  auth
  {
    UIntStorage[record] = value;
    bytes32[] memory records = new bytes32[](1);
    records[0] = record;
    uint[] memory values = new uint[](1);
    values[0] = value;
    emit EternalDbEvent(records, values, block.timestamp);
  }

  function setUIntValues(bytes32[] memory records, uint[] memory values) public
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      UIntStorage[records[i]] = values[i];
    }
    emit EternalDbEvent(records, values, block.timestamp);
  }

  function deleteUIntValue(bytes32 record) public
  auth
  {
    delete UIntStorage[record];
  }

  ////////////
  //Strings
  ////////////

  mapping(bytes32 => string) StringStorage;

  function getStringValue(bytes32 record) view public returns (string memory){
    return StringStorage[record];
  }

  function setStringValue(bytes32 record, string memory value) public
  auth
  {
    StringStorage[record] = value;
  }

  function deleteStringValue(bytes32 record) public
  auth
  {
    delete StringStorage[record];
  }

  ////////////
  //Address
  ////////////

  mapping(bytes32 => address) AddressStorage;

  function getAddressValue(bytes32 record) view public returns (address){
    return AddressStorage[record];
  }

  function setAddressValues(bytes32[] memory records, address[] memory values) public
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      AddressStorage[records[i]] = values[i];
    }
  }

  function setAddressValue(bytes32 record, address value) public
  auth
  {
    AddressStorage[record] = value;
  }

  function deleteAddressValue(bytes32 record) public
  auth
  {
    delete AddressStorage[record];
  }

  ////////////
  //Bytes
  ////////////

  mapping(bytes32 => bytes) BytesStorage;

  function getBytesValue(bytes32 record) view public returns (bytes memory){
    return BytesStorage[record];
  }

  function setBytesValue(bytes32 record, bytes memory value) public
  auth
  {
    BytesStorage[record] = value;
  }

  function deleteBytesValue(bytes32 record) public
  auth
  {
    delete BytesStorage[record];
  }

  ////////////
  //Bytes32
  ////////////

  mapping(bytes32 => bytes32) Bytes32Storage;

  function getBytes32Value(bytes32 record) view public returns (bytes32){
    return Bytes32Storage[record];
  }

  function getBytes32Values(bytes32[] memory records) view public returns (bytes32[] memory results){
    results = new bytes32[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = Bytes32Storage[records[i]];
    }
  }

  function setBytes32Value(bytes32 record, bytes32 value) public
  auth
  {
    Bytes32Storage[record] = value;
  }

  function setBytes32Values(bytes32[] memory records, bytes32[] memory values) public
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      Bytes32Storage[records[i]] = values[i];
    }
  }

  function deleteBytes32Value(bytes32 record) public
  auth
  {
    delete Bytes32Storage[record];
  }

  ////////////
  //Boolean
  ////////////

  mapping(bytes32 => bool) BooleanStorage;

  function getBooleanValue(bytes32 record) view public returns (bool){
    return BooleanStorage[record];
  }

  function getBooleanValues(bytes32[] memory records) view public returns (bool[] memory results){
    results = new bool[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = BooleanStorage[records[i]];
    }
  }

  function setBooleanValue(bytes32 record, bool value) public
  auth
  {
    BooleanStorage[record] = value;
  }

  function setBooleanValues(bytes32[] memory records, bool[] memory values) public
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      BooleanStorage[records[i]] = values[i];
    }
  }

  function deleteBooleanValue(bytes32 record) public
  auth
  {
    delete BooleanStorage[record];
  }

  ////////////
  //Int
  ////////////
  mapping(bytes32 => int) IntStorage;

  function getIntValue(bytes32 record) view public returns (int){
    return IntStorage[record];
  }

  function getIntValues(bytes32[] memory records) view public returns (int[] memory results){
    results = new int[](records.length);
    for (uint i = 0; i < records.length; i++) {
      results[i] = IntStorage[records[i]];
    }
  }

  function setIntValue(bytes32 record, int value) public
  auth
  {
    IntStorage[record] = value;
  }

  function setIntValues(bytes32[] memory records, int[] memory values) public
  auth
  {
    for (uint i = 0; i < records.length; i++) {
      IntStorage[records[i]] = values[i];
    }
  }

  function deleteIntValue(bytes32 record) public
  auth
  {
    delete IntStorage[record];
  }

}
