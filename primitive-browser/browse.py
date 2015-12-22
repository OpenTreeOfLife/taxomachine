#!/usr/bin/python

# Brutally primitive reference taxonomy browser.
# Basically just a simple shim on the taxomachine 'taxon' method.
# Intended to be run as a CGI command, but it can be tested by running it 
# directly from the shell; just set the environment QUERY_STRING to be
# the name or id of the taxon to browse to.

# Apache (or other server) must be configured to be able to run CGI scripts, 
# and this program must be in the directory where it looks for such scripts.
# The file name there should be simply 'browse' (not browse.py).

# NOT YET IMPLEMENTED: percent-escaping and -unescaping

# If this were to be written using peyotl, it might do something similar to the following:
# from peyotl.api import APIWrapper
# taxo = APIWrapper().taxomachine
# print taxo.taxon(12345)

api_base_url = 'https://devapi.opentreeoflife.org/'

import os
import sys
import cgi, cgitb

import requests
import simplejson

headers = {
    'content-type' : 'application/json',
    'accept' : 'application/json',
}

def link_to_taxon(id, text, limit=None):
    if limit == None:
        option = ''
    else:
        option = '&limit=%s' % limit
    return "<a href='browse?id=%s%s'>%s</a>" % (id, option, text)

def link_to_name(name):
    return "<a href='browse?name=%s'>%s</a>" % (name, name)

def browse_by_name(name):
    result = look_up_name(name)
    matches = result[u'matches']
    if len(matches) == 0:
        print 'no TNRS matches for %s' % name
        return None
    elif len(matches) > 1:
        sys.stdout.write('Matches for %s: \n' % name)
        for match in matches:
            sys.stdout.write("  %s\n" % link_to_taxon(match[u'ot:ottId'], match[u'unique_name']))
    else:
        id = matches[0][u'ot:ottId']
        browse_by_id(id)

def look_up_name(name):
    response = requests.post(api_base_url + 'v2/tnrs/match_names',
                             headers=headers,
                             data=simplejson.dumps({'names':[name]}))
    response.raise_for_status()
    answer = response.json()
    results = answer[u'results']
    if len(results) == 0: return None
    if len(results) > 1:
        print 'multiple results - should not happen'
        return None # shouldn't happen
    return results[0]

def browse_by_id(id, limit=None):
    info = get_taxon_info(id)
    #print simplejson.dumps(info, sort_keys=True, indent=4)
    display_taxon_info(info, limit)
    sys.stdout.write("\n<a href='https://github.com/OpenTreeOfLife/reference-taxonomy/wiki/Taxon-flags'>explanation of flags</a>\n");

def get_taxon_info(ottid):
    d=simplejson.dumps({'ott_id': ottid, 'include_children': True, 'include_lineage': True})
    response = requests.post(api_base_url + 'v2/taxonomy/taxon',
                             headers=headers,
                             data=d)
    response.raise_for_status()
    return response.json()

def display_taxon_info(info, limit=None):
    if u'ot:ottId' in info:
        sys.stdout.write("Taxon: ")
        display_basic_info(info)

        if u'synonyms' in info:
            synonyms = info[u'synonyms']
            name = info[u'ot:ottTaxonName']
            if name in synonyms:
                synonyms.remove(name)
            if len(synonyms) > 0:
                sys.stdout.write("Synonym(s): %s\n" % ', '.join(map(link_to_name, synonyms)))
        if u'taxonomic_lineage' in info:
            first = True
            sys.stdout.write('Lineage: ')
            for ancestor in info[u'taxonomic_lineage']:
                if not first:
                    sys.stdout.write(' &lt; ')
                sys.stdout.write(link_to_taxon(ancestor[u'ot:ottId'], ancestor[u'ot:ottTaxonName']))
                first = False
            sys.stdout.write('\n')
        else:
            print 'missing lineage field', info.keys()
        if u'children' in info:
            children = sorted(info[u'children'], key=priority)
            if len(children) > 0:
                sys.stdout.write('Children:\n')
                i = 0
                if limit == None: limit = 200
                for child in children:
                    if ishidden(child):
                        sys.stdout.write("* ")
                    else:
                        sys.stdout.write("  ")
                    display_basic_info(child)
                    i += 1
                    if i > limit:
                        sys.stdout.write('... %s\n' % link_to_taxon(info[u'ot:ottId'],
                                                                    ('%s more children' %
                                                                     (len(children)-limit)),
                                                                    limit=100000))
                        break
    else:
        print '? losing'
        print simplejson.dumps(info, sort_keys=True, indent=4)

def display_basic_info(info):
    # Might be better to put rank as a separate column in a table.  That way the
    # names will line up
    if not info[u'rank'].startswith('no rank'):
        sys.stdout.write(info[u'rank'] + ' ')

    # Taxon name
    if u'unique_name' in info and len(info[u'unique_name']) > 0:
        text = info[u'unique_name']
    elif u'ot:ottTaxonName' in info:
        text = info[u'ot:ottTaxonName']
    sys.stdout.write(link_to_taxon(info[u'ot:ottId'], text))

    # Sources and flags
    if u'tax_sources' in info:
        sys.stdout.write(' %s ' % ', '.join(map(source_link, info[u'tax_sources'])))
    sys.stdout.write('%s ' % ', '.join(map(lambda f:f.lower(), info[u'flags'])))
    sys.stdout.write('\n')

def source_link(source_id):
    parts = source_id.split(':')
    url = None
    if len(parts) == 2:
        if parts[0] == 'ncbi':
            url = 'http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=%s' % parts[1]
        elif parts[0] == 'gbif':
            url = 'http://www.gbif.org/species/%s/' % parts[1]
        elif parts[0] == 'irmng':
            url = 'http://www.marine.csiro.au/mirrorsearch/ir_search.taxon_info?id=%s' % parts[1]
        elif parts[0] == 'if':
            url = 'http://www.indexfungorum.org/names/NamesRecord.asp?RecordID=%s' % parts[1]
        elif parts[0] == 'worms':
            url = 'http://www.marinespecies.org/aphia.php?p=taxdetails&id=%s' % parts[1]
        elif parts[0] == 'silva':
            url = 'http://www.arb-silva.de/browser/ssu/silva/%s' % parts[1]
    if url != None:
        return '<a href="%s">%s</a>' % (url, source_id)
    else:
        return source_id

def priority(child):
    if ishidden(child):
        return 1
    else:
        return 0

def ishidden(info):
    for flag in info[u'flags']:
        if flag in ott29_exclude_flags:
            return True
    return False

# From treemachine/src/main/java/opentree/GraphInitializer.java

ott29_exclude_flags_list = ["major_rank_conflict", "major_rank_conflict_inherited", "environmental",
		"unclassified_inherited", "unclassified", "viral", "barren", "not_otu", "incertae_sedis",
		"incertae_sedis_inherited", "extinct_inherited", "extinct", "hidden", "unplaced", "unplaced_inherited",
		"was_container", "inconsistent", "inconsistent", "hybrid", "merged", "inconsistent"]
ott29_exclude_flags = {}
for flag in ott29_exclude_flags_list:
    ott29_exclude_flags[flag.upper()] = True


print 'Content-type: text/html'
print
print '<pre>'

form = cgi.FieldStorage()
limit = None
if "limit" in form:
    limit = int(form["limit"].value)
if "name" in form:
    browse_by_name(form["name"].value)
elif "id" in form:
    browse_by_id(int(form["id"].value), limit=limit)
else:
    print "bogus invocation"

print '</pre>'
