import json, pycurl, sys
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

class RequestSender():
    def __init__(self):
        self.json_storage = StringIO()
        self.c = pycurl.Curl()    

    def execute(self, url, data):
        self.json_storage.truncate(0)
        self.c.setopt(self.c.URL, url)
        self.c.setopt(self.c.HTTPHEADER, ["Content-type:Application/json"])
        self.c.setopt(self.c.WRITEFUNCTION, self.json_storage.write)
        self.c.perform()
        return self
        
    @property
    def response(self):
        return self.json_storage

if __name__ == "__main__":

    r = RequestSender()
    
    url = "http://localhost:7474/db/data/ext/{}"

    for service, data in taxonomy_tests:
        data = r.execute(url.format(service),json.dumps(data)).response
        try:
            json.loads(data)
        except:
            sys.exit(1)
        sys.stderr("passed: " + service)