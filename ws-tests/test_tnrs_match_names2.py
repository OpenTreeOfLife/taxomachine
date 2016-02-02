#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/tnrs/match_names'
TEST_LIST = ["Hylobates"]
TEST_IDS = [166552]
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"names":TEST_LIST, "context_name": 'Mammals'},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if set(TEST_LIST) != set(result[u'matched_name_ids']):
    errstr = "Failed to match, submitted: {}, returned {}\n"
    sys.stderr.write(errstr.format(TEST_LIST,result[u'matched_name_ids']))
    sys.exit(1)
MATCH_LIST = result['results']
for match in MATCH_LIST:
    m = match[u'matches'][0]
    if m[u'matched_name'] not in TEST_LIST:
        errstr = "bad match return {}, expected one of {}\n"
        sys.stderr.write(errstr.format(m[u'matched_name'],str(TEST_LIST)))
        sys.exit(1)
    if m[u'ot:ottId'] not in TEST_IDS:
        errstr = "bad match return {}, expected one of {}\n"
        sys.stderr.write(errstr.format(m[u'ot:ottId'],str(TEST_IDS)))
        sys.exit(1)
