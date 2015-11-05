#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/taxon'
r = test_http_json_method(SUBMIT_URI,
                          'POST',
                          data={"ott_id":1066581, "list_terminal_descendants":"true"},
                          expected_status=200,
                          return_bool_data=True)
if not r[0]:
    sys.exit(1)
descendants = r[1][u'terminal_descendants']
if not set([490099,1066590]).issubset(set(descendants)):
    sys.stderr.write("Bos taurus (490099) and Bos primigenius (1066590) not returned as descendants of Bos (1066581)")
    sys.exit(1)


