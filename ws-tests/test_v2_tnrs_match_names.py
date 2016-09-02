#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/tnrs/match_names'
TEST_LIST = ["Aster","Symphyotrichum","Erigeron","Barnadesia"]
TEST_IDS = [5507594,1058735,643717,515698]
test, result = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"names":TEST_LIST},
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
    if m.get(u'ot:ottId') not in TEST_IDS:
        print m
        errstr = "bad match return {}, expected one of {}\n"
        sys.stderr.write(errstr.format(m.get(u'ot:ottId'),str(TEST_IDS)))
        sys.exit(1)
    if m.get(u'matched_name') not in TEST_LIST:
        errstr = "bad match return {}, expected one of {}\n"
        sys.stderr.write(errstr.format(m.get(u'matched_name'),str(TEST_LIST)))
        sys.exit(1)
