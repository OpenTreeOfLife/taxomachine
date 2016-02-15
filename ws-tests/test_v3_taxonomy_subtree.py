#!/usr/bin/env python
import sys, os, re
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/subtree'
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"ott_id":515698},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
tree = result[u'subtree']
if tree is None or tree == '':
    sys.stderr.write('Expected result to have a tree, but found {}\n'.format(result))
    sys.exit(1)
# trying to avoid dealing with a newick parser here...
ROOTTAXONSTR = "\)Barnadesia_ott515698;"
namecheck =  re.compile(ROOTTAXONSTR)
if re.search(namecheck,tree) is None:
    errstr = 'requested taxon {} does not appear at root of tree:\n {}'
    sys.stderr.write(errstr,ROOTTAXONSTR[1:],tree)
    sys.exit(1)
