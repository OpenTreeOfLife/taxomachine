#!/usr/bin/env python

# export PYTHONPATH=~/a/ot/repo/germinator/ws-tests
# python ws-tests/test_v3_addition.py host:apihost="http://localhost:7476" host:translate=true

import sys, requests, opentreetesting, json

from check import *

opentreetesting.exit_if_api_is_readonly('test_v3_addition.py')

# Find an unused OTT id

id = 99000000

DOMAIN = opentreetesting.config('host', 'apihost')

while True:
    response = requests.request('POST',
                                opentreetesting.translate(DOMAIN + '/v3/taxonomy/taxon_info'),
                                headers={'content-type' : 'application/json'},
                                data=json.dumps({'ott_id': id}),
                                allow_redirects=True)
    if response.status_code != 200:
        print 'id %s is unused' % id
        break
    print 'id %s is in use' % id
    id += 10

name1 = "Test taxon 1"
name2 = "Test taxon 2"

additions_doc = {
    "taxa": [{"ott_id": id,
              "name": name1,
              "rank": "species",
              "parent": 4133636},
             {"ott_id": id+1,
              "name": "Test taxon 2",
              "rank": "genus",
              "parent": 4133636}],
    "study_id": "no study, testing",
    "id": "additions-%s-%s" % (id, id+1)}

status = simple_test('/v3/taxonomy/process_additions',
                     {"addition_document": json.dumps(additions_doc)})
if status != 0: sys.exit(status)

status = simple_test('/v3/taxonomy/taxon_info',
                     {"ott_id": id},
                     is_right=(lambda r:r[u"name"] == name1))
if status != 0: sys.exit(status)
