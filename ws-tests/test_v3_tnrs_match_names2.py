#!/usr/bin/env python

from check import *

# Simple unambiguous match

TEST_LIST = ["Gerbera"]  # was Hylobates
TEST_IDS = [1021694]  # was 166552

def check_result(result):
    # Should match only one taxon
    if set(TEST_LIST) != set(result[u'matched_names']):
        errstr = "** failed to match, submitted: {}, returned {}\n"
        sys.stderr.write(errstr.format(TEST_LIST,result[u'matched_names']))
        return False
    # Only one name requested
    match_result = result['results'][0]
    if match_result[u'name'] != TEST_LIST[0]:
        print '** expected', TEST_LIST[0], 'but got', match_result[u'name']
        return False
    m = match_result[u'matches'][0]
    if m[u'matched_name'] not in TEST_LIST:
        errstr = "** bad match return {}, expected one of {}\n"
        sys.stderr.write(errstr.format(m[u'matched_name'],str(TEST_LIST)))
        return False
    ott_id = m[u'taxon'][u'ott_id']
    if ott_id not in TEST_IDS:
        errstr = "** bad match return {}, expected one of {}\n"
        sys.stderr.write(errstr.format(ott_id, str(TEST_IDS)))
        return False
    return True

status = 0

status += \
simple_test('/v3/tnrs/match_names',
            {u'names': TEST_LIST, u'context_name': 'All life'},
            check_match_names_result,
            check_result)

sys.exit(status)
