#!/usr/bin/python

import shlex, subprocess, sys

cmd = "mvn --quiet compile assembly:single test"
p = subprocess.Popen(shlex.split(cmd),stdout=subprocess.PIPE,stderr=subprocess.PIPE)
r = p.communicate()
print r[0], r[1]

for line in r[0].split("\n"):
    if "Tests run:" in line:
        parts = line.split(": ")
        passed, failed, errored, skipped = [int(p.split(",")[0]) for p in parts[1:5]]
        sys.exit(failed + errored)