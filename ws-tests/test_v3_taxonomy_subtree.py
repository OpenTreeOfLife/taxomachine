#!/usr/bin/env python

from check import *

status = 0

def check_result(result):
    return (u')Barnadesia_ott515698;' in result[u'newick'])

status += \
simple_test('/v3/taxonomy/subtree',
            {u'ott_id': 515698},
            check=check_blob([field(u'newick', check_string)]),
            is_right=check_result)

sys.exit(status)
