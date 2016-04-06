See ws-tests/README.md in the phylesystem-api repo for information on the test harness.

ws-tests/README.md in the germinator talks about the super-tester that
calls this.

To run against devapi, do

    ./run_tests.sh host:apihost=https://devapi.opentreeoflife.org

To run an individual test, do

    python test_individual_test.py host:apihost=https://devapi.opentreeoflife.org

or if you like

   for t in test_*.py; do echo $t; python $t host:apihost=https://devapi.opentreeoflife.org; done


The tests can be run locally against neo4j without having to go
through apache.  peyotl needs to be installed to run the tests.  The
command would be something like this:

    ./run_tests.sh host:apihost=http://localhost:7474 host:translate=true

substituting the correct port number agreeing with your neo4j
configuration (default 7474, changed on deployed servers to 7476 for
taxomachine or 7478 for oti).
