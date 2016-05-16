#!/usr/bin/env python

from check import *

TEST_NAME = u'Alseuosmia banksii'

def check_result(x):
    return x[u'name'] == TEST_NAME

status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'source_id': u'ncbi:490635'},
            check_extended_taxon_blob,
            check_result)

sys.exit(status)
