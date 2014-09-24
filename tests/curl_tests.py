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
    ('tnrs/graphdb/match_names', {"names":["Aster","Symphyotrichum","Erigeron","Barnadesia"]}),
    ('tnrs/graphdb/autocomplete_name', {"name":"Endoxyla","context_name":"All life"}),
    ('tnrs/graphdb/contexts', {}),
    ('tnrs/graphdb/infer_context', {"names":["Pan","Homo","Mus","Bufo","Drosophila"]}),
]

url = "http://{s}/db/data/ext/{r}"
server = os.environ['TAXOMACHINE_SERVER']
#sys.stderr.write("server: "+server)
#sys.stderr.flush()
#exit()

def run_test():

    for service, data in tests:
        yield exec_call, service, data

def exec_call(service, data):

    service_url = url.format(s=server,r=service)
    sys.stderr.write("\ncurl -X POST " + service_url + " -H 'content-type:application/json' -d '" + json.dumps(data) + "'")

    try:
        r = requests.post(service_url, json.dumps(data))
        d = json.loads(r.text)
        
        # need to check for stacktrace
        
        if 'error' not in d:
            sys.stderr.write("ok\n")
        else:
            sys.stderr.write('error: ' + d['error'])
            assert False

    except Exception as ex:
        sys.stderr.write(ex)
        assert False
    

