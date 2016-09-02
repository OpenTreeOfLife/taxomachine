#!/usr/bin/env python
from check import *

NAMESLIST = [u'Pan', u'Homo', u'Mus musculus', u'Upupa epops']

def check_result(result):
    if result[u'context_name'] != u'Tetrapods':
        errstr = '** expected context = Tetrapods, returned {}\n'
        sys.stderr.write(errstr.format(result[u'context_name']))
        return False
    if result[u'ambiguous_names'] != []:
        errstr = '** expected no ambiguous_names, but found {}\n'
        sys.stderr.write(errstr.format(result[u'ambiguous_names']))
        return False
    return True

status = 0

status += \
simple_test('/v3/tnrs/infer_context',
            {u'names': NAMESLIST},
            check_blob([field(u'context_name', check_string),
                        field(u'context_ott_id', check_integer),
                        field(u'ambiguous_names', check_list(check_string))]),
            is_right=check_result)

sys.exit(status)
