#!/usr/bin/env python

from check import *

status = 0

status += \
simple_test('/v3/taxonomy/mrca',
            {u'ott_ids': [901642, 55033]},
            check_blob([field('mrca', check_extended_taxon_blob)]))

if False:
    expectedId = 637370
    if mrca[u'ott_id'] != expectedId:
        errstr = 'Expected {} , found {}\n'
        sys.stderr.write(errstr.format(expectedId,mrca[u'ott_id']))
        sys.exit(1)

sys.exit(status)
