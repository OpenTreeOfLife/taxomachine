Run the following command to test the basic functionality of the services:

```
./test.sh
```

You can point this script at different servers by setting the TAXOMACHINE_SERVER environment variable in your shell before running the script. For example:

```bash
# to test devapi remotely
TAXOMACHINE_SERVER=devapi.opentreeoflife.org/taxomachine && export TAXOMACHINE_SERVER && ./test.sh
```

```bash
# to test locally on a machine where the taxomachine neo4j instance is running on an nonstandard port (in this case 7476)
TAXOMACHINE_SERVER=localhost:7476/db/data && export TAXOMACHINE_SERVER && ./test.sh
```