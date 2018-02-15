pragma solidity ^0.4.18;

library Convert {
  function bytesToBytes32(bytes b, uint offset) internal pure returns (bytes32) {
    bytes32 out;

    for (uint i = 0; i < 32; i++) {
      out |= bytes32(b[offset + i] & 0xFF) >> (i * 8);
    }
    return out;
  }
}