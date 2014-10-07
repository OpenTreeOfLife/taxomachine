# set the TAXOMACHINE_SERVER environment variable in your shell to point the script at the right location, e.g.:
#
# TAXOMACHINE_SERVER=devapi.opentreeoflife.org/taxomachine # to run remotely against devapi
#
# TAXOMACHINE_SERVER=localhost:7476/db/data # to run locally on devapi

# test services on localhost if no other location is specified
[ -z "$TAXOMACHINE_SERVER" ] && TAXOMACHINE_SERVER='localhost:7474/db/data' && export TAXOMACHINE_SERVER

cd tests && nosetests -vs ServiceTests.py

# capture output and return number of failed/errored tests