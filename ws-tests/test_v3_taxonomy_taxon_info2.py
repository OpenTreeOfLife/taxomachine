#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/taxon_info'
TEST_NAME = u'Alseuosmia banksii'
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"ott_id":901642},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'name' not in result:
    sys.stderr.write('name not returned in result\n')
    sys.exit(1)
taxonName = result[u'name']
if taxonName != TEST_NAME:
    errstr = 'Expected taxon name {} but not found in \n{}\n'
    sys.stderr.write(errstr.format(TEST_NAME,taxonName))
    sys.exit(1)

