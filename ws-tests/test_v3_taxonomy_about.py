#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/about'
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'source' not in result:
    sys.stderr.write('No source reported in \n{}'.format(result))
    sys.exit(1)
if u'author' not in result:
    sys.stderr.write('No author reported in \n{}'.format(result))
    sys.exit(1)
if u'weburl' not in result:
    sys.stderr.write('No weburl reported in \n{}'.format(result))
    sys.exit(1)
