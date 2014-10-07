## java unit tests

type mvn >/dev/null 2>&1 || { echo >&2 "maven is required for testing but could not be found. Aborting."; exit 1; }

echo "Running Java unit tests"
tests/run_unit_tests.sh

## service tests

type virtualenv >/dev/null 2>&1 || { echo >&2 "virtualenv is required for testing but could not be found. Aborting."; exit 1; }

virtualenv test_venv
. test_venv/bin/activate || { echo "could not activate virtualenv for testing. Aborting."; exit 1; }

type nosetests >/dev/null 2>&1 || { echo "installing test dependencies"; ./install_dependencies_for_tests.sh; }
python -c "import requests" >/dev/null 2>&1 || { echo "installing test dependencies"; ./install_dependencies_for_tests.sh; }

echo "Running Neo4j service tests"
tests/run_service_tests.sh