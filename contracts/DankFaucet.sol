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

  // An event for letting the caller know that they didn't supply enough
  // ETH to complete the transaction.
  event NotEnoughETH(string description, uint ethRequired);

  // An event for communicating success/failure of the steps of the
  // verification process, including calls to the Oraclize service.
  event DankEvent(bytes32 hashedPhoneNumber, bool successful, string description);

  // An event for communicating the id from Oraclize and a message.
  event OraclizeCall(bytes32 id, string msg);

  // An event fired when a person's allotment is reset by the faucet.
  event DankReset(bytes32 hashedPhoneNumber);

  /**
   * Sets the initial allotment of DANK tokens, the number of tokens
   * a person gets for initially singing up. Expects DANK values in wei.
   */
  constructor(uint initialDank) public {
    allotment = initialDank;

    // Comment out this line when deploying to production
    // See: https://github.com/oraclize/ethereum-bridge
    //OAR = OraclizeAddrResolverI(0x6f485C8BF6fc43eA212E93BBF8ce046C7f1cb475);
  }

  /**
   * If this person doesn't already have an initial allotment of Dank this function calls the
   * second half of the phone number verification API. If their phone number checks out it
   * transfers them an allotment of Dank that was set by the constructor.
   */
  function verifyAndAcquireDank(bytes32 hashedPhoneNumber, string encryptedPayload) public {
    if (oraclize_getPrice("URL") > address(this).balance) {
      emit NotEnoughETH("Oraclize query for phone number verification was NOT sent, add more ETH.", oraclize_getPrice("URL"));
    } else {
      bytes32 queryId = oraclize_query("nested", getOraclizeQuery(encryptedPayload));
      phoneNumberRequests[queryId] = PhoneNumberRequest(msg.sender, hashedPhoneNumber);
    }
  }

  /**
   * Callback fuction for reqeusts to Oraclize. The name and signature are
   * required by Oraclize.
   */
  function __callback(bytes32 queryId, string result) public {
    emit OraclizeCall(queryId, result);

    if (msg.sender != oraclize_cbAddress()) {
        revert("The sender's address does not match Oraclize's address.");
    }
    else {
        PhoneNumberRequest storage phoneNumberRequest = phoneNumberRequests[queryId];
        uint previouslyAllocatedDank = allocatedDank[phoneNumberRequest.hashedPhoneNumber];

        if ((previouslyAllocatedDank <= 0) && (dankToken.balanceOf(address(this)) >= allotment)) {
          bool dankTransfered = dankToken.transfer(phoneNumberRequest.sender, allotment);
          if (dankTransfered) {
            allocatedDank[phoneNumberRequest.hashedPhoneNumber] = allotment;
            emit DankEvent(phoneNumberRequest.hashedPhoneNumber, dankTransfered, "DANK transfered");
          }
        }
        else {
          emit DankEvent(phoneNumberRequest.hashedPhoneNumber, false, "DANK already allocated.");
        }
    }
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
    emit DankReset(hashedPhoneNumber);
  }

  /**
   * Allow the owner to reset the limits on the amount distributed by the faucet.
   */
  function resetAllotment(uint initialDank) auth {
    allotment = initialDank;
  }

  /**
   * Allow the owner to withdraw DANK in this contract
   */
  function withdrawDank() auth {
    dankToken.transfer(msg.sender, dankToken.balanceOf(this));
  }

  /**
   * Allow the owner to withdraw DANK in this contract
   */
  function withdrawEth() auth {
    msg.sender.transfer(address(this).balance);
  }
}
