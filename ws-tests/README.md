See ws-tests/README.md in the phylesystem-api repo for information on the test harness.

ws-tests/README.md in the germinator talks about the super-tester that
calls this.

You can use python modules from this repo or from germinator:

    export PYTHONPATH=lib

or

    export PYTHONPATH=../../germinator/ws-tests

To run the tests against devapi, do

    ./run_tests.sh host:apihost=https://devapi.opentreeoflife.org

To run an individual test, do

    python test_individual_test.py host:apihost=https://devapi.opentreeoflife.org

or if you like

   for t in test_*.py; do echo $t; python $t host:apihost=https://devapi.opentreeoflife.org; done


The tests can be run locally against neo4j without having to go
through apache.  To start neo4j, do 'make run'.  Then, the command to
test would be something like this:

    ./run_tests.sh host:apihost=http://localhost:7474 host:translate=true

substituting the correct port number agreeing with your neo4j
configuration (default is 7474, but the Makefile changes it to 7476).
