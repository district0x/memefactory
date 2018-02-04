#!/usr/bin/env bash
cd resources/public/contracts/src

function solc-err-only {
    solc "$@" 2>&1 | grep -A 2 -i "Error"
}

solc-err-only --overwrite --optimize --bin --abi Registry.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi MemeFactory.sol -o ../build/

cd ../build
wc -c AttributeStore.bin | awk '{print "AttributeStore: " $1}'
wc -c DLL.bin | awk '{print "DLL: " $1}'
wc -c Parameterizer.bin | awk '{print "Parameterizer: " $1}'
wc -c PLCRVoting.bin | awk '{print "PLCRVoting: " $1}'
wc -c Registry.bin | awk '{print "Registry: " $1}'
wc -c MemeFactory.bin | awk '{print "MemeFactory: " $1}'
