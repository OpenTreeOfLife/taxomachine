#!/usr/bin/env python

from check import *

def check_result(result):
    return result[u'ott_id'] == 515698

status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'ott_id': 515698, u'include_lineage': True},
            check_blob(extended_taxon_blob_fields +
                       [field(u'lineage', check_list(check_extended_taxon_blob))]),
            check_result)

sys.exit(status)

