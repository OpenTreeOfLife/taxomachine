#!/usr/bin/env python

from check import *

TEST_NAME = u'Alseuosmia banksii'

def check_result(result):
    sources = result[u'tax_sources']
    if u'ncbi:58228' not in sources:
        errstr = 'Expected ncbi taxon id (58228) not found in tax_sources\n {}\n'
        sys.stderr.write(errstr.format(sources))
        return False
    if u'gbif:3189571' not in sources:
        errstr = 'Expected gbif taxon id (3189571) not found in tax_sources\n {}'
        sys.stderr.write(errstr.format(sources))
        return False
    if u'irmng:11346207' not in sources:
        errstr = 'Expected irmng taxon id (11346207) not found in tax_sources\n {}'
        sys.stderr.write(errstr.format(sources))
        return False
    return True


status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'ott_id': 766177},
            check_extended_taxon_blob,
            check_result)

sys.exit(status)
