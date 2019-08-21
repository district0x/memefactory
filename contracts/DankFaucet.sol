pragma solidity ^0.4.24;
import "./oraclize/ethereum-api/oraclizeAPI.sol";
import "./DankToken.sol";
import "./auth/DSAuth.sol";
import "./utils/strings.sol";

/***
 * A Faucet for getting initial DANK tokens. Provide a phone number,
 * reveive a one-time allotment of DANK.
 ***/
contract DankFaucet is usingOraclize, DSAuth {
  using strings for *;

  DankToken public constant dankToken = DankToken(0xDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaDDeaD);

  // A request to allocate dank to a phone number.
  struct PhoneNumberRequest {
    address sender;
    bytes32 hashedPhoneNumber;
  }

  // Tracks the DANK allocated to a hashed phone number.
  mapping(bytes32 => uint) public allocatedDank;

  // A mapping between the Oraclize query id and a combination of the
  // hashed phone number and the sender so the callback from Oraclize
  // can track who's been allocated what DANK.
  mapping(bytes32 => PhoneNumberRequest) public phoneNumberRequests;

  // The number of DANK tokens a person gets when they initially sing up.
  uint public allotment;

  // An event for communicating successful transfer of funds
  event DankTransferEvent(string message, bytes32 hashedPhoneNumber);

  // An event for communicating the id from Oraclize and a message.
  event OraclizeRequestEvent(string message, bytes32 id);

  event OraclizeResponseEvent(string message, bytes32 id, string response);

  // An event fired when a person's allotment is reset by the faucet.
  event DankResetEvent(bytes32 hashedPhoneNumber);

  /**
   * Sets the initial allotment of DANK tokens. Expects DANK values in wei.
   */
  constructor(uint _allotment) public {
    allotment = _allotment;

    // Comment out this line when deploying to production
    // See: https://github.com/oraclize/ethereum-bridge
    //OAR = OraclizeAddrResolverI(0xd0a6D832Ea2949B87165B2e8CE7119013c835295);
  }

  /**
   * If this person doesn't already have an initial allotment of DANK this function calls the
   * second half of the phone number verification API. If their phone number checks out Oraclize
   * executes __callback transaction.
   */
  function verifyAndAcquireDank(bytes32 hashedPhoneNumber, string encryptedPayload) public {

    uint dankBalance = dankToken.balanceOf(address(this));
    require(dankBalance >= allotment, "Faucet has run out of DANK");

    uint previouslyAllocatedDank = allocatedDank[hashedPhoneNumber];
    require(previouslyAllocatedDank <= 0, "DANK already allocated");

    uint ethBalance = address(this).balance;
    require(oraclize_getPrice("URL") > ethBalance, "Oraclize query for phone number verification was NOT sent, add more ETH.");

    bytes32 queryId = oraclize_query("nested", getOraclizeQuery(encryptedPayload));
    phoneNumberRequests[queryId] = PhoneNumberRequest(msg.sender, hashedPhoneNumber);

    emit OraclizeRequestEvent("Oraclize query was sent, standing by for the response", queryId);
  }

  /**
   * Callback function for requests to Oraclize. The name and signature are
   * required by Oraclize.
   */
  function __callback(bytes32 queryId, string result) public {
    emit OraclizeResponseEvent("Oraclize query response", queryId, result);

    require(msg.sender != oraclize_cbAddress(), "The sender's address does not match Oraclize's address");
    require(!result.toSlice().contains("\"success\":true".toSlice()), "Wrong verification code");

    PhoneNumberRequest storage phoneNumberRequest = phoneNumberRequests[queryId];

    require(dankToken.transfer(phoneNumberRequest.sender, allotment), "DANK transfer failed");
    allocatedDank[phoneNumberRequest.hashedPhoneNumber] = allotment;

    emit DankTransferEvent("DANK transferred", phoneNumberRequest.hashedPhoneNumber);
  }

  function getOraclizeQuery(string payload) constant returns(string) {
    var parts = new strings.slice[](3);
    parts[0] = "[computation] ['QmdKK319Veha83h6AYgQqhx9YRsJ9MJE7y33oCXyZ4MqHE', 'GET', 'https://api.authy.com/protected/json/phones/verification/check', '${[decrypt] ".toSlice();
    parts[1] = payload.toSlice();
    parts[2] = "}']".toSlice();
    return "".toSlice().join(parts);
  }

  /**
   * Returns the amount of ETH this contract has available. ETH is used to pay for
   * having Oraclize call the external API for validating phone numbers.
   */
  function getBalance() public view returns (uint256) {
      return address(this).balance;
  }

  /**
   * Simple function to allow for adding ETH to the contract.
   */
  function sendEth() public payable { }

  /**
   * Allow the owner to reset the DANK we've allocated to a phone number.
   */
  function resetAllocatedDankForPhoneNumber(bytes32 hashedPhoneNumber) auth {
    delete allocatedDank[hashedPhoneNumber];
    emit DankResetEvent(hashedPhoneNumber);
  }

  /**
   * Allow the owner to reset the limits on the amount distributed by the faucet.
   */
  function resetAllotment(uint _allotment) auth {
    allotment = _allotment;
  }

  /**
   * Allow the owner to withdraw DANK in this contract
   */
  function withdrawDank() auth {
    dankToken.transfer(msg.sender, dankToken.balanceOf(this));
  }

  /**
   * Allow the owner to withdraw ETH in this contract
   */
  function withdrawEth() auth {
    msg.sender.transfer(address(this).balance);
  }
}
