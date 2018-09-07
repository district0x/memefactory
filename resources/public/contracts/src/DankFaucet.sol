pragma solidity ^0.4.24;
import "github.com/oraclize/ethereum-api/oraclizeAPI.sol";

/***
 * A Faucet for getting initial DANK tokens. Provide a phone number,
 * reveive a one-time allotment of DANK.
 ***/
contract DankFaucet is usingOraclize {

  // A phone number's current balance of DANK. This contract isn't the
  // definitive source of DANK, however we are tracking it here for
  // support reasons so we can see how much DANK each phone number was
  // gifted.
  mapping(bytes32 => uint) balances;
  
  // The number of DANK tokens a person gets when they initially sing up.
  uint public allotment;
  
  // An event for letting the caller know that they didn't supply enough
  // ETH to complete the transaction.
  event NotEnoughETH(string description, uint ethRequired);
  
  // An event for communicating messages around the phone number
  // verification process, including calls to the Oraclize service.
  event PhoneNumberVerification(string description);
 
  // An event for communicating the id from Oraclize and a message.
  event OraclizeCall(bytes32 id, string msg);
  
  /**
   * Sets the initial allotment of DANK tokens, the number of tokens
   * a person gets for initially singing up. That allotment can 
   */
  constructor(uint initialDank) public {
    allotment = initialDank;
    
    // Uncomment this for deploying to production
    // OAR = OraclizeAddrResolverI(oraclizeAddress);
    
    // Remove this line out for production
    OAR = OraclizeAddrResolverI(0x9e2c43153aa0007e6172af3733021a227480f008);
  }

  /**
   * Returns the amount of DANK associated with a particular phone number.
   * Returns 0 if the phone number has not been given any DANK.
   */
  function getAllocatedDank(string phoneNumber) public view returns (uint)
  {
    bytes32 hashedNumber = keccak256(abi.encodePacked(phoneNumber));
    return balances[hashedNumber];
  }
  
  /**
   * Callback fuction for reqeusts to Oraclize. The name and signature are
   * required by Oraclize.
   */ 
  function __callback(bytes32 myid, string result) public {
    emit OraclizeCall(myid, result);
    
    emit PhoneNumberVerification("Checking the sender contract");
    if (msg.sender != oraclize_cbAddress()) {
        emit PhoneNumberVerification("The sender's address does not match Oraclize's address.");
        revert();
    }    
    else {
        emit PhoneNumberVerification("Successful call to the phone number verification API.");
        
        // Set a placeholder in the balances mapping?
        
    }
  }
    
  /**
   * Associates DANK with a phone number by sending it to the calling account.
   * Does nothing if the phone number already has DANK associated with it.
   */
  function acquireInitialDank(string phoneNumber) public payable returns (uint)
  {
    if (oraclize_getPrice("URL") > this.balance) {
      emit NotEnoughETH("Oraclize query for phone number verification was NOT sent, add more ETH.", oraclize_getPrice("URL"));
    } else {
      emit PhoneNumberVerification("Starting the phone number verification process.");

      //oraclize_query("nested", "[computation] ['QmdKK319Veha83h6AYgQqhx9YRsJ9MJE7y33oCXyZ4MqHE', 'POST', 'https://api.authy.com/protected/json/phones/verification/start', '{\"json\": {\"via\": \"sms\", \"phone_number\": \"3036815821\", \"country_code\": 1}, \"headers\": {\"content-type\": \"application/json\", \"X-Authy-API-Key\": \"yBa6JnyJ2CC3k1dTEhON2uxCWvRXB2rk\"}}']");
      oraclize_query("nested", "[computation] ['QmdKK319Veha83h6AYgQqhx9YRsJ9MJE7y33oCXyZ4MqHE', 'POST', 'https://api.authy.com/protected/json/phones/verification/start', '${[decrypt] BJJDeoDK+rtjDV/1zEm75+LP0ST9B0OiQdSwVpzMOJt1mRVH0rJ2QrcdnZy5cfdg/amRmHqSX9SJ4OEUO5K5rUu/NBs3eLy6OcHuNedrmlizwcKvlC+9h6/5Grw2jsHTnoXCOS2XMRyD7Xk5AWNMY3J9mw+92kJD1Jxrr3MjXWF3DqgaCwTj+Ec1mec1PVKndw1d05zWFdxpPE75elpg5+nexoK7zFIdHbqGC4AxHMtkwDs7H4q36ltBqSZ8+jPMij//5p2vCfEK3zCFxqBRac6EQTkcRY71IwaLiT+nnrqaQQtfSFyui5f9O+PEiuIHHVmyxRpocU7OGQj0ieTKSjvOxana1KSSoBFYRUu6xrfzOSouw7wNow==}']");
      
      bytes32 hashedNumber = keccak256(abi.encodePacked(phoneNumber));
      balances[hashedNumber] = allotment;
    }
  }
}
