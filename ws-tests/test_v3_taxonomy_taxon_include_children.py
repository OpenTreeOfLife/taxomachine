#!/usr/bin/env python

from check import *

def check_result(result):
    return 503056 in map(lambda c:c[u'ott_id'], result[u'children'])

status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'ott_id': 515698, u'include_children': True},
            check_blob(extended_taxon_blob_fields +
                       [field(u'children', check_list(check_extended_taxon_blob))]),
            check_result)

sys.exit(status)
