#!/usr/bin/env python
import sys, os
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/mrca'
# added 3826 from asterales
test, _, _ = test_http_json_method(SUBMIT_URI,
                                   "POST",
                                   data={"ott_ids":[5551856,821970,770319]},
                                   expected_status=400,
                                   return_bool_data=True)

if not test:
    sys.exit(1)
