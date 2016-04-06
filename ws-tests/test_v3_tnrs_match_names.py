#!/usr/bin/env python

from check import *

TEST_LIST = ["Aster","Symphyotrichum","Erigeron","Barnadesia"]
TEST_IDS = [5507594,1058735,643717,515698]

status = 0

status += \
simple_test('/v3/tnrs/match_names',
            {u'names': TEST_LIST},
            check_match_names_result)

sys.exit(status)
