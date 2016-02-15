#!/usr/bin/env python
import sys, os, re
from opentreetesting import test_http_json_method, config
DOMAIN = config('host', 'apihost')
SUBMIT_URI = DOMAIN + '/v3/taxonomy/subtree'
# 3826 = Argophyllaceae which is in Asterales
test, result, _ = test_http_json_method(SUBMIT_URI, "POST",
                                        data={"ott_id":3826},
                                        expected_status=200,
                                        return_bool_data=True)
if not test:
    sys.exit(1)
if 'subtree' not in result:
    sys.stderr.write("Expected 'subtree' in result but didn't find it")
    sys.exit(1)
tree = result[u'subtree']
if tree is None or tree == '':
    sys.stderr.write("Expected result to have a tree, but found {}\n".format(result))
    sys.exit(1)
# trying to avoid dealing with a newick parser here...
ROOTTAXONSTR = "\)Argophyllaceae_ott3826;"
DESCENDANTTAXONSTR = "\,Canis_lycaon_ott948004\,"
namecheck =  re.compile(ROOTTAXONSTR)
namecheck2 = re.compile(DESCENDANTTAXONSTR)
if re.search(namecheck,tree) is None:
    errstr = "requested taxon{} does not appear at root of tree\n {}\n"
    sys.stderr.write(errstr.format(ROOTTAXONSTR[1:],tree))
    sys.exit(1)
if re.search(namecheck2,tree) is None:
    errstr = "expected terminal taxon {} does not appear in tree\n {}\n"
    sys.stderr.write(errstr.format(DESCENDANTTAXONSTR[2:],tree))
    sys.exit(1)
