#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/mrca'
# added 3826 from asterales
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"ott_ids":[5551856,821970,770319]},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'mrca' not in result:
    sys.stderr.write('No mrca found in result {}\n'.format(result))
    sys.exit(1)
mrca = result[u'mrca']
if u'ott_id' not in mrca:
    sys.stderr.write('No ottId found in result {}\n'.format(result))
    sys.exit(1)
expectedId = 770319
if mrca.get(u'ott_id') != expectedId:
    sys.stderr.write('Expected {}, found {}\n'.format(expectedId,mrca.get(u'ott_id')))
    sys.exit(1)
if u'ott_ids_not_found' not in result:
    errstr = "Expected to find list of ott_ids_not_found, but didn't in {}\n"
    sys.stderr.write(errstr,result)
    sys.exit(1)
badids = result[u'ott_ids_not_found']
expectedBadId = 5551856
if expectedBadId not in badids:
    errstr = 'Expected to find {} in bad ids, found {}\n'
    sys.stderr.write(errstr.format(expectedBadId,badids))
    sys.exit(1)


