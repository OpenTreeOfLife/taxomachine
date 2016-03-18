#!/usr/bin/env python

import re
from check import *

SEARCHNAME = "Endoxyla"
NAMECHECK = re.compile(SEARCHNAME)

def check_result(result):
    if len(result) == 0:
        sys.stderr.write("Empty list returned\n")
        return False
    for item in result:
        uname = item[u'unique_name']
        if not re.search(NAMECHECK, uname):
            failstr = "unique name: {} of taxon record does not contain search name: {}\n"
            sys.stderr.write(failstr.format(uname, SEARCHNAME))
            return False
    return True

status = 0

status += \
simple_test('/v3/tnrs/autocomplete_name',
            {u'name': SEARCHNAME, u'context_name': u'All life'},
            check_list(check_blob([field(u'ott_id', check_integer),
                                   field(u'unique_name', check_string),
                                   field(u'is_suppressed', check_boolean),
                                   field(u'is_higher', check_boolean)])),
            check_result)

sys.exit(status)
