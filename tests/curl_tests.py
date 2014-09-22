import json, requests, sys

tests = [
    
    # taxonomy
    ('taxonomy/graphdb/about', {}),
    ('taxonomy/graphdb/lica', {"ott_ids":[515698,590452,409712,643717]}),
    ('taxonomy/graphdb/subtree', {"ott_id":515698}),
    ('taxonomy/graphdb/taxon', {"ott_id":515698}),
    ('taxonomy/graphdb/flags', {}),
    ('taxonomy/graphdb/deprecated_taxa', {}),

    # tnrs
    ('tnrs/graphdb/match_names', {"names":["Aster","Symphyotrichum","Erigeron","Barnadesia"]}),
    ('tnrs/graphdb/autocomplete_name', {"name":"Endoxyla","context_name":"All life"}),
    ('tnrs/graphdb/contexts', {}),
    ('tnrs/graphdb/infer_context', {"names":["Pan","Homo","Mus","Bufo","Drosophila"]}),
]

url = "http://localhost:7474/db/data/ext/{}"

def run_test():

    for service, data in tests:
        yield exec_call, service, data

def exec_call(service, data):

    sys.stderr.write(service + " " + json.dumps(data) + "...")

    try:
        r = requests.post(url.format(service), json.dumps(data))
        d = json.loads(r.text)
        if 'error' not in d:
            sys.stderr.write("ok\n")
        else:
            sys.stderr.write('error: ' + d['error'])
            assert False

    except Exception as ex:
        sys.stderr.write(ex)
        assert False
    

