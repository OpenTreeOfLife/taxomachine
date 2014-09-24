import json, os, requests, sys

tests = [
    
    # taxonomy
    ('taxonomy/graphdb/about', {}),
    ('taxonomy/graphdb/lica', {"ott_ids":[5551856,821970,770319]}), # canidae
    ('taxonomy/graphdb/lica', {"ott_ids":[515698,590452,409712,643717]}), # asterales
    ('taxonomy/graphdb/subtree', {"ott_id":515698}),
    ('taxonomy/graphdb/subtree', {"ott_id":372706}), # canis
    ('taxonomy/graphdb/taxon', {"ott_id":515698, "include_lineage":True}),
    ('taxonomy/graphdb/taxon', {"ott_id":766177}), # garcinia mangostana

    ('taxonomy/graphdb/flags', {}),
    ('taxonomy/graphdb/deprecated_taxa', {}),

    # tnrs
    ('tnrs_v2/graphdb/match_names', {"names":["Aster","Symphyotrichum","Erigeron","Barnadesia"]}),
    ('tnrs_v2/graphdb/autocomplete_name', {"name":"Endoxyla","context_name":"All life"}),
    ('tnrs_v2/graphdb/contexts', {}),
    ('tnrs_v2/graphdb/infer_context', {"names":["Pan","Homo","Mus","Bufo","Drosophila"]}),
]

url = "http://{s}/ext/{r}"
server = os.environ['TAXOMACHINE_SERVER']

def run_test():

    for service, data in tests:
        yield exec_call, service, data

def exec_call(service, data):

    service_url = url.format(s=server,r=service)
    sys.stderr.write("\ncurl -X POST " + service_url + " -H 'content-type:application/json' -d '" + json.dumps(data) + "'")
    sys.stderr.flush()

    try:
        r = requests.post(service_url, json.dumps(data))
        d = json.loads(r.text)
            
        # check for error returned by service itself
        if 'error' in d:
            sys.stderr.write('error: ' + d['error'] + '\n')
            assert False
    
        # check for java exception
        elif 'exception' in d:
            print d
            sys.stderr.write('exception: ' + d['fullname'] + '\n' + d['stacktrace'] + '\n')
            assert False
    
    # check for json parsing exception or problem calling service
    except Exception as ex:
        sys.stderr.write(e.message)
        assert False
    
    sys.stderr.flush()

