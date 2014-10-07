#!/usr/bin/env python

import os, shlex, subprocess, sys

"""Set the TAXOMACHINE_SERVER environment variable in your shell to
point the script at the right location, e.g.:

to run remotely against devapi
TAXOMACHINE_SERVER=devapi.opentreeoflife.org/taxomachine && export TAXOMACHINE_SERVER

to run locally on devapi
TAXOMACHINE_SERVER=localhost:7476/db/data && export TAXOMACHINE_SERVER"""

# test services on localhost if no other location is specified
if os.environ.get('TAXOMACHINE_SERVER') == None:
    os.environ['TAXOMACHINE_SERVER'] = 'localhost:7474/db/data'

os.chdir("tests")
cmd = "nosetests -vs ServiceTests.py"
p = subprocess.Popen(shlex.split(cmd),stdout=subprocess.PIPE,stderr=subprocess.PIPE)
r = p.communicate()
print r[0],r[1]

for line in r[1].split("\n"):
    if "FAILED" in line:
        print line
        parts = line.split("=")
        failed = int(parts[1].strip(")"))
        sys.exit(failed)

sys.exit(0)