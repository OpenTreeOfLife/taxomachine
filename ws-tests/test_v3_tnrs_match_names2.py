#!/usr/bin/env python

from check import *

TEST_LIST = ["Hylobates"]
TEST_IDS = [166552]

def check_result(result):
    if set(TEST_LIST) != set(result[u'matched_names']):
        errstr = "Failed to match, submitted: {}, returned {}\n"
        sys.stderr.write(errstr.format(TEST_LIST,result[u'matched_names']))
        return False
    for match in result['results']:
        m = match[u'matches'][0]
        if m[u'matched_name'] not in TEST_LIST:
            errstr = "bad match return {}, expected one of {}\n"
            sys.stderr.write(errstr.format(m[u'matched_name'],str(TEST_LIST)))
            return False
        if m[u'ott_id'] not in TEST_IDS:
            errstr = "bad match return {}, expected one of {}\n"
            sys.stderr.write(errstr.format(m[u'ott_id'],str(TEST_IDS)))
            return False
    return True

status = 0

status += \
simple_test('/v3/tnrs/match_names',
            {u'names': TEST_LIST, u'context_name': 'Mammals'},
            check_match_names_result,
            check_result)

sys.exit(status)
