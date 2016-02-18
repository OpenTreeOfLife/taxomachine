#!/bin/bash

# This is just an example test script for your inspiration - you may
# want to do things differently.  This assumes you have neo4j set up
# to run locally.

set -e

./compile_server_plugins.sh

neo4j-community-1.9.5/bin/neo4j stop
cp -p target/taxomachine-neo4j-plugins-0.0.1-SNAPSHOT.jar neo4j-community-1.9.5/plugins/
neo4j-community-1.9.5/bin/neo4j start

cd ws-tests

./run_tests.sh host:apihost=http://localhost:7474 host:translate=true

# or `python test_blahblah.py` to run one test
