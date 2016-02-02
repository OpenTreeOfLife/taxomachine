#!/usr/bin/env python
import sys, os, re
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v2/tnrs/autocomplete_name'
SEARCHNAME = "Endoxyla"
test, result, _= test_http_json_method(SUBMIT_URI, "POST",
                                       data={"name":SEARCHNAME,"context_name":"All life"},
                                       expected_status=200,
                                       return_bool_data=True)
if not test:
    sys.exit(1)
if result == []:
    sys.stderr.write("Empty list returned\n")
    sys.exit(1)
NAMECHECK = re.compile(SEARCHNAME)
for item in result:
    uname = item[u'unique_name']
    if uname is None:
        sys.stderr.write("returned taxon record had no unique_name specified\n")
        sys.exit(1)
    elif not re.search(NAMECHECK, uname):
        failstr = "unique name: {} of taxon record does not contain search name: {}\n"
        sys.stderr.write(failstr.format(uname, SEARCHNAME))
        sys.exit(1)
sys.exit(0)

