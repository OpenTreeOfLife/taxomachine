#!/usr/bin/env python

# Change to:
#  Gerbera, Gerbera jamesonii, Gerbera cordata
#  1021694, 1021698, 435564

taxon = 1021694

from check import *

def check_result(result):
    if not set([1021698, 435564]).issubset(set(result[u'terminal_descendants'])):
        sys.stderr.write("hamesonii (1021698) and cordata (435564) not returned as descendants of Gerbera (1021694)\n")
        return False
    return True

status = 0

status += \
simple_test('/v3/taxonomy/taxon_info',
            {u'ott_id': taxon, u'include_terminal_descendants': True},
            check_blob(extended_taxon_blob_fields +
                       [field(u'terminal_descendants', check_list(check_integer))]),
            check_result)

sys.exit(status)
