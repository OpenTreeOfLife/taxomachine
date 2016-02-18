#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/taxon'
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"ott_id":766177},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if u'tax_sources' not in result:
    sys.stderr.write('No tax_sources found in result\n')
    sys.exit(1)
sources = result[u'tax_sources']
if u'ncbi:58228' not in sources:
    errstr = 'Expected ncbi taxon id (58228) not found in tax_sources\n {}'
    sys.stderr.write(errstr.format(sources))
    sys.exit(1)
if u'gbif:3189571' not in sources:
    errstr = 'Expected gbif taxon id (3189571) not found in tax_sources\n {}'
    sys.stderr.write(errstr.format(sources))
    sys.exit(1)
if u'irmng:11346207' not in sources:
    errstr = 'Expected irmng taxon id (11346207) not found in tax_sources\n {}'
    sys.stderr.write(errstr.format(sources))
    sys.exit(1)

