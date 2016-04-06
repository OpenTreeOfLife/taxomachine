#!/usr/bin/env python

from check import *

# Argophyllaceae is a family in Asterales

status = 0

def check_result(result):
    newick = result[u'newick']
    if not u')Argophyllaceae_ott3826;' in newick:
        print '** Argophyllaceae_ott3826; not in newick'
        print
        print newick
        print
        return False
    if not u',Argophyllum_nitidum_ott5746190,' in newick:
        print '** Argophyllum_nitidum_ott5746190, not in newick'
        print
        print newick
        print
        return False
    return True

status += \
simple_test('/v3/taxonomy/subtree',
            {u'ott_id': 3826},
            check_blob([field('newick', check_string)]),
            check_result)

sys.exit(status)
