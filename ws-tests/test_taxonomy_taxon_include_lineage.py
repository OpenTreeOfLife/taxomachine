#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/taxonomy/taxon'
test, result, _ = test_http_json_method(SUBMIT_URI,
                                        data={"ott_id":515698, "include_lineage":"true"},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if result[u'ot:ottId'] != 515698:
    sys.stderr.write('Incorrect ottid in returned taxon {}',format(result[u'ot:ottId']))
    sys.exit(1)
if u'taxonomic_lineage' not in result:
    errstr = 'No lineage returned when expected in taxon report: {}'
    sys.stderr.write(errstr.format(result))
    sys.exit(1)
