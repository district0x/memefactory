pragma solidity ^0.4.24;

/* A Faucet for getting initial DANK tokens. Provide a phone number,
   reveive a one-time allotment of DANK. */
contract DankFaucet {

  mapping(string => uint) balances;
  uint public initialDank = 8675;

  constructor(uint allotment) public {
    initialDank = allotment;
  }

  function getDank(string phoneNumber) public returns (uint)
  {
    return balances[phoneNumber];
  }

  function acquireInitialDank(string phoneNumber) public returns (uint)
  {
    balances[phoneNumber] = initialDank;
    return initialDank;
  }
}
