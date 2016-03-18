#!/usr/bin/env python

from check import *

status = 0

def check_result(result):
    return (u')Barnadesia_ott515698;' in result[u'newick'])

status += \
simple_test('/v3/taxonomy/subtree',
            {u'ott_id': 515698},
            check_blob([field('newick', check_string)]),
            check_result)

sys.exit(status)
