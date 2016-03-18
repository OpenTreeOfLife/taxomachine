#!/usr/bin/env python

from check import *

def check_result(result):
    if not set([490099, 1066590]).issubset(set(result[u'terminal_descendants'])):
        sys.stderr.write("Bos taurus (490099) and Bos primigenius (1066590) not returned as descendants of Bos (1066581)\n")
        return False
    return True

status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'ott_id': 1066581, u'list_terminal_descendants': True},
            check_blob(extended_taxon_blob_fields +
                       [field(u'terminal_descendants', check_list(check_integer))]),
            check_result)

sys.exit(status)
