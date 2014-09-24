[ -z "$TAXOMACHINE_SERVER" ] && TAXOMACHINE_SERVER='localhost:7474/db/data' && export TAXOMACHINE_SERVER
cd tests && nosetests -vs curl_tests.py