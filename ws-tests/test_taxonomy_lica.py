#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/lica'
test, result, _ = test_http_json_method(SUBMIT_URI,
                                        data={"ott_ids":[515698,590452,409712,643717]},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'lica' not in result:
    sys.stderr.write('No lica found in \n{}'.format(result))
    sys.exit(1)
lica = result[u'lica']
if u'ot:ottId' not in lica:
    sys.stderr.write('No ott id returned in lica \n{}'.format(result))
    sys.exit(1)
expectedId = 1042120
if lica[u'ot:ottId'] != expectedId:
    sys.stderr.write('Expected , found {}'.format(lica[u'ottId']))
    sys.exit(1)

