#!/usr/bin/env python

from check import *

# OTT id 901642, in Asterales
TEST_NAME = u'Alseuosmia banksii'
TEST_ID = 901642

def check_result(result):
    sources = result[u'tax_sources']
    if u'ncbi:490635' not in sources:
        errstr = 'Expected ncbi taxon id (58228) not found in tax_sources\n {}\n'
        sys.stderr.write(errstr.format(sources))
        return False
    if u'gbif:7326528' not in sources:
        errstr = 'Expected gbif taxon id (3189571) not found in tax_sources\n {}'
        sys.stderr.write(errstr.format(sources))
        return False
    return True


status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'ott_id': TEST_ID},
            check_extended_taxon_blob,
            is_right=check_result)

sys.exit(status)
