#!/usr/bin/env python

from check import *

status = 0

# In asterales system maybe this should be 46248 (family Asteraceae)?
expectedId = 1042120  # order Asterales

status += \
simple_test('/v3/taxonomy/mrca',
            {u'ott_ids': [513931,3899965,3895873,4727679]},
            check_blob([field('mrca', check_extended_taxon_blob)]),
            is_right=lambda result: result[u'mrca'][u'ott_id'] in [expectedId, 46248])

sys.exit(status)
