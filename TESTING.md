The `test.sh` script will run a set of basic tests against the taxomachine web services. The current tests are simple and relatively brittle, and were designed using ott 2.8, so some may fail against other versions of the taxonomy.

**IMPORTANT:** these tests must be run against a neo4j server connected to a valid OTT database. By default, the script will look for the services on the local machine at the default neo4j port (7474):

```bash
# to test against localhost:7474
./test.sh
```

You can point this script at different servers by setting the TAXOMACHINE_SERVER environment variable in your shell before running the script. This variable needs to indicate the server as well as any path components preceding the 'ext/...' in the service parth. For example:

```bash
# to test devapi remotely
TAXOMACHINE_SERVER=devapi.opentreeoflife.org/taxomachine && export TAXOMACHINE_SERVER && ./test.sh
```

```bash
# to test locally on a machine where the taxomachine neo4j instance is running on a
# nonstandard port (in this case 7476)
TAXOMACHINE_SERVER=localhost:7476/db/data && export TAXOMACHINE_SERVER && ./test.sh
```
