#!/usr/bin/env bash
cd resources/public/contracts/src

function solc-err-only {
    solc "$@" 2>&1 | grep -A 2 -i "Error"
}

solc-err-only --overwrite --optimize --bin --abi RegistryFactory.sol -o ../build/

cd ../build
wc -c Registry.bin | awk '{print "Registry: " $1}'
wc -c RegistryFactory.bin | awk '{print "RegistryFactory: " $1}'
wc -c RegistryEntry.bin | awk '{print "RegistryEntry: " $1}'
wc -c RegistryEntryToken.bin | awk '{print "RegistryEntryToken: " $1}'
