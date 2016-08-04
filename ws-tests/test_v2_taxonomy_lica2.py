#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/lica'
test, result = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"ott_ids":[901642, 55033]},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'lica' not in result:
    sys.stderr.write('No lica found in returned: \n{}'.format(result))
    sys.exit(1)
lica = result[u'lica']
if u'ot:ottId' not in lica:
    sys.stderr.write('No ottId found in returned: \n{}'.format(result))
    sys.exit(1)
expectedId = 637370
if lica[u'ot:ottId'] != expectedId:
    errstr = 'Expected {} , found {}\n'
    sys.stderr.write(errstr.format(expectedId,lica[u'ot:ottId']))
    sys.exit(1)

