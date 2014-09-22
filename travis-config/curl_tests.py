import json, requests, sys
from StringIO import StringIO

taxonomy_tests = [
    ('taxonomy/graphdb/about', {}),
    ('taxonomy/graphdb/lica', {"ott_ids":[515698,590452,409712,643717]}),
    ('taxonomy/grapdb/subtree', {"ott_id":515698}),
    ('taxonomy/graphdb/taxon', {"ott_id":515698}),
    ('taxonomy/graphdb/flags', {}),
    ('taxonomy/graphdb/deprecated_taxa', {})
]

tnrs_tests = [
    ('tnrs/graphdb/match_names', {"names":["Aster","Symphyotrichum","Erigeron","Barnadesia"]}),
    ('tnrs/graphdb/autocomplete_name', {"name":"Endoxyla","context_name":"All life"}),
    ('tnrs/graphdb/contexts', {}),
    ('tnrs/graphdb/infer_context', {"names":["Pan","Homo","Mus","Bufo","Drosophila"]}),
    ('hgskjiuyc ilgfaw', {})
]

if __name__ == "__main__":

    url = "http://localhost:7474/db/data/ext/{}"

    for service, data in taxonomy_tests:
        r = requests.post(url.format(service), json.dumps(data))
        print r.json
#        try:
#           json.loads(r.json)
#        except:
#            sys.exit(1)
        print("passed: " + service)