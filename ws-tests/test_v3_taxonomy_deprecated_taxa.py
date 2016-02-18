#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/deprecated_taxa'
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                          expected_status=200,
                          return_bool_data=True)
if not test:
    sys.exit(1)

