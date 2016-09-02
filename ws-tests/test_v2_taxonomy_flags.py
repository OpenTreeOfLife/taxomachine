#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/flags'
test, result = test_http_json_method(SUBMIT_URI, "POST",
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
# result is large dictionary.  Will do a minimal test
if u'incertae_sedis' not in result:
    sys.stderr.write('No reported count of incertae_sedis taxa\n')
    sys.exit(1)
if u'unclassified' not in result:
    sys.stderr.write('No reported count of unclassified taxa\n')
    sys.exit(1)
