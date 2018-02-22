#!/usr/bin/env bash
cd resources/public/contracts/src

function solc-err-only {
    solc "$@" 2>&1 | grep -A 2 -i "Error"
}

solc-err-only --overwrite --optimize --bin --abi DankToken.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi MemeFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi ParameterChangeFactory.sol -o ../build/
solc-err-only --overwrite --optimize --bin --abi MutableForwarder.sol -o ../build/

cd ../build
wc -c MutableForwarder.bin | awk '{print "MutableForwarder: " $1}'
wc -c Registry.bin | awk '{print "Registry: " $1}'
wc -c MemeFactory.bin | awk '{print "MemeFactory: " $1}'
wc -c Meme.bin | awk '{print "Meme: " $1}'
wc -c MemeToken.bin | awk '{print "MemeToken: " $1}'
wc -c ParameterRegistry.bin | awk '{print "ParameterRegistry: " $1}'
wc -c ParameterChangeFactory.bin | awk '{print "ParameterChangeFactory: " $1}'
wc -c ParameterChange.bin | awk '{print "ParameterChange: " $1}'
