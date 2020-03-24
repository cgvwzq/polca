#!/usr/bin/env bash

export JAVA_ARGS="$@"
mvn -q exec:java -e -Dexec.mainClass=net.vwzq.polca.Polca -Dexec.args="${JAVA_ARGS}"
