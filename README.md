# MemeFactory

Create and trade provably rare digital assets on the Ethereum blockchain

See at [https://memefactory.io](https://memefactory.io/)

Smart-contracts can be found [here](https://github.com/district0x/memefactory/tree/master/resources/public/contracts/src).  
Following diagram shows interaction flow of MemeFactory smart-contracts. For further explanations, feel free to read comments in smart-contract files. 
![MemeFactory SmartContracts](https://user-images.githubusercontent.com/3857155/36697475-00a2fc00-1afc-11e8-9b72-9a308d6e85d6.png)

## Development
Start server: 
```bash
ganache-cli -p 8549
lein repl
(start-server!)
node dev-server/memefactory.js
```

Start UI:
```bash
lein repl
(start-ui!)
# go to http://localhost:4598/
```

Start tests:
```bash
ganache-cli -p 8549
lein test-dev
```