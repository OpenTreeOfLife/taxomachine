#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/deprecated_taxa'
# currently returns an empty list
test, result, _ = test_http_json_method(SUBMIT_URI,
                                        expected_status=200,
                                        expected_response=[],
                                        return_bool_data=True)
if not test:
    sys.exit(1)

