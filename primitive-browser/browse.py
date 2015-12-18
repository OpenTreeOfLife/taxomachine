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

import requests
import simplejson

headers = {
    'content-type' : 'application/json',
    'accept' : 'application/json',
}

def browse_by_name(name):
    result = look_up_name(name)
    matches = result[u'matches']
    if len(matches) == 0:
        print 'no TNRS matches for %s' % name
        return None
    elif len(matches) > 1:
        sys.stdout.write('Matches for %s: \n' % name)
        for match in matches:
            sys.stdout.write("  <a href='browse?%s'>%s</a>\n" % (match[u'ot:ottId'], match[u'unique_name']))
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

def browse_by_id(id):
    info = taxon_info(id)
    #print simplejson.dumps(info, sort_keys=True, indent=4)
    display_info(info, True)

def taxon_info(ottid):
    d=simplejson.dumps({'ott_id': ottid, 'include_children': True, 'include_lineage': True})
    response = requests.post(api_base_url + 'v2/taxonomy/taxon',
                             headers=headers,
                             data=d)
    response.raise_for_status()
    return response.json()

def display_info(info, longp):
    if u'ot:ottId' in info:
        if ishidden(info): sys.stdout.write("* ")

        name = info[u'ot:ottTaxonName']

        # Main info line
        sys.stdout.write("<a href='browse?%s'>" % info[u'ot:ottId'])
        if not info[u'rank'].startswith('no rank'):
            sys.stdout.write("%s " % info[u'rank'])
        if u'unique_name' in info and len(info[u'unique_name']) > 0:
            sys.stdout.write("%s" % info[u'unique_name'])
        elif u'ot:ottTaxonName' in info:
            sys.stdout.write("%s" % name)
        sys.stdout.write("</a> ")

        if u'tax_sources' in info:
            sys.stdout.write('%s ' % ', '.join(map(source_link, info[u'tax_sources'])))
        sys.stdout.write('%s ' % ', '.join(map(lambda f:f.lower(), info[u'flags'])))
        sys.stdout.write('\n')
        if longp:
            if u'synonyms' in info:
                synonyms = info[u'synonyms']
                if name in synonyms:
                    synonyms.remove(name)
                if len(synonyms) > 0:
                    sys.stdout.write("  a.k.a. %s\n" % ', '.join(synonyms))
            if u'taxonomic_lineage' in info:
                first = True
                sys.stdout.write('  ')
                for ancestor in info[u'taxonomic_lineage']:
                    if not first:
                        sys.stdout.write(' &lt; ')
                    sys.stdout.write('<a href="browse?%s">%s</a>' % (ancestor[u'ot:ottId'], ancestor[u'ot:ottTaxonName']))
                    first = False
                sys.stdout.write('\n')
            else:
                print 'missing lineage field', info.keys()
            if u'children' in info:
                sys.stdout.write('\n')
                children = sorted(info[u'children'], key=priority)
                i = 0
                for child in children:
                    display_info(child, False)
                    i += 1
                    if i > 20: 
                        sys.stdout.write('... %s more children\n' % (len(children)-20))
                        break
    else:
        print '? losing'
        print simplejson.dumps(info, sort_keys=True, indent=4)

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

name_or_id = os.environ.get('QUERY_STRING')

if name_or_id.isdigit():
    browse_by_id(int(name_or_id))
else:
    browse_by_name(name_or_id)

print '</pre>'
