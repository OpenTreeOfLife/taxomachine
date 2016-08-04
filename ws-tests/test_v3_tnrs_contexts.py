#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/tnrs/contexts'
test, result = test_http_json_method(SUBMIT_URI, "POST",
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if not (u'PLANTS' in result and u'ANIMALS' in result and
        u'FUNGI' in result and u'LIFE' in result and u'MICROBES' in result):
    errstr = 'Missing key in context listing \n {} \n'
    sys.stderr.write(errstr.format(result))
    sys.exit(1)        
if u'Archaea' not in result[u'MICROBES']:
    sys.stderr.write('Archaea not in context MICROBES\n')
    sys.exit(1)
if u'Arachnides' not in result[u'ANIMALS']:  # spelling?
    sys.stderr.write('Arachnides not in context Animals\n')
    sys.exit(1)
