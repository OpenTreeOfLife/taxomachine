#!/usr/bin/python

import shlex, subprocess

p = subprocess.Popen(shlex.split("mvn --quiet compile assembly:single test"),stdout=subprocess.PIPE,stderr=subprocess.PIPE)
#p.execute()
r = p.communicate()
print r[0], r[1]

for line in r[0].split("\n"):
    if "Tests run:" in line:
        parts = line.split(": ")
        print parts
        passed, failed, errored, skipped = [int(p.split(",")[0]) for p in parts[1:5]]
        return failed + errored            
