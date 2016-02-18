#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/mrca'
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"ott_ids":[901642, 55033]},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'mrca' not in result:
    sys.stderr.write('No mrca found in returned: \n{}'.format(result))
    sys.exit(1)
mrca = result[u'mrca']
if u'ott_id' not in mrca:
    sys.stderr.write('No ottId found in returned: \n{}'.format(result))
    sys.exit(1)
expectedId = 637370
if mrca[u'ott_id'] != expectedId:
    errstr = 'Expected {} , found {}\n'
    sys.stderr.write(errstr.format(expectedId,mrca[u'ott_id']))
    sys.exit(1)

