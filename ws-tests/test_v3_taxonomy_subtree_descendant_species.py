#!/usr/bin/env python

from check import *

status = 0

def check_result(result):
    return (u'\)Argophyllaceae_ott3826;' in result and
            u'\,Argophyllum_nitidum_ott5746190\,' in result)

status += \
simple_test('/v3/taxonomy/subtree',
            {u'ott_id': 3826},
            check_blob([field('newick', check_string)]),
            check_result)

sys.exit(status)
