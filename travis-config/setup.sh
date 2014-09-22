#!/bin/bash
set -x
if test -z "${NO_VIRT_ENV_INSTALL}"
then
    if true # ! which virtualenv 2>/dev/null
    then
        if true # ! which pip 2>/dev/null
        then
            curl -o pip-1.5.6.tar.gz 'https://pypi.python.org/packages/source/p/pip/pip-1.5.6.tar.gz#md5=01026f87978932060cc86c1dc527903e' || exit
            tar xfvz pip-1.5.6.tar.gz || exit
            cd pip-1.5.6/
            python setup.py install || exit
            cd -
        fi
        pip install virtualenv || exit
    fi

    virtualenv env || exit
    source env/bin/activate || exit
    pip install requests
    pip install nose
fi