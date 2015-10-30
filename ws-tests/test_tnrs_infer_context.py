#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/tnrs/infer_context'
NAMESLIST = ["Pan","Homo","Mus","Bufo","Drosophila"]
test, result, _ = test_http_json_method(SUBMIT_URI,
                                        data={"names":NAMESLIST},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if result[u'context_name'] != u'Animals':
    errstr = 'Expected context = Animals, returned {}'
    sys.stderr.write(errstr.format(result[u'context_name']))
if result[u'ambiguous_names'] != []:
    errstr = 'Expected no ambiguous_names, but found {}'
    sys.stderr.write(errstr.format(result[u'ambiguous_names']))

