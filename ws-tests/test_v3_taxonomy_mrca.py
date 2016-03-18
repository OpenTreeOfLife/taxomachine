#!/usr/bin/env python

from check import *

status = 0

status += \
simple_test('/v3/taxonomy/mrca',
            {u'ott_ids': [513931,3899965,3895873,4727679]},
            check_blob([field('mrca', check_extended_taxon_blob)]),
            lambda result: result[u'mrca'][u'ott_id'] == expectedId)

sys.exit(status)
