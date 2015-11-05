#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/lica'
test, result, _ = test_http_json_method(SUBMIT_URI,
                                        data={"ott_ids":[5551856,821970,770319]},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'lica' not in result:
    sys.stderr.write('No lica found in result {}'.format(result))
    sys.exit(1)
lica = result[u'lica']
if u'ot:ottId' not in lica:
    sys.stderr.write('No ottId found in result {}'.format(result))
    sys.exit(1)
expectedId = 770319
if lica[u'ot:ottId'] != expectedId:
    sys.stderr.write('Expected {}, found {}'.format(expectedId,lica[u'ottId']))
    sys.exit(1)
if u'ott_ids_not_found' not in result:
    errstr = "Expected to find list of ott_ids_not_found, but didn't in {}"
    sys.stderr.write(errstr,result)
    sys.exit(1)
badids = result[u'ott_ids_not_found']
expectedBadId = 5551856
if expectedBadId not in badids:
    errstr = 'Expected to find {} in bad ids, found {}'
    sys.stderr.write(errstr.format(expectedBadId,badids))
    sys.exit(1)


