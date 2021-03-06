#!/usr/bin/env python

from check import *

TEST_NAME = u'Alseuosmia banksii'

def check_result(x):
    return x[u'name'] == TEST_NAME

status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'ott_id': 901642},
            check_extended_taxon_blob,
            is_right=check_result)

sys.exit(status)
