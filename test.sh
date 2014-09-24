[ -z "$TAXOMACHINE_SERVER" ] && TAXOMACHINE_SERVER='localhost:7474'
cd tests && nosetests -vs curl_tests.py