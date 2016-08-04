#!/usr/bin/env python

from check import *

# 'SAR324 clade(Marine group B)'
TEST_ID=5247929

status = 0

def check_result(result):
    return ((u"'SAR324 clade(Marine group B)_ott%s'" % TEST_ID) in result[u'newick'])

status += \
simple_test('/v3/taxonomy/subtree',
            {u'ott_id': TEST_ID},
            check_blob([field(u'newick', check_string)]),
            is_right=check_result)

sys.exit(status)
