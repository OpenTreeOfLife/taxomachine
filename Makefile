# This Makefile does two things
#   1. Creates a taxomachine database ready for deployment (needs lots of RAM)
#   2. Sets up a server running locally for testing purposes

#  $^ = all prerequisites
#  $< = first prerequisite
#  $@ = file name of target

# 1. Create database

TARBALL=$(shell echo "taxomachine-`date '+%Y%m%d%n'`.db.tgz")

all: $(TARBALL)

db: taxomachine.db

# This one will have to be configured manually for development versions of the taxonomy.
# Get 'released' taxonomy from http://files.opentreeoflife.org/ott/current/ott.tgz
TAXONOMY=ott
$(TAXONOMY):
	curl http://files.opentreeoflife.org/ott/current/ott.tgz >ott.tgz
	rm -rf ott.tmp && mkdir ott.tmp && cd ott.tmp && tar xzf ../ott.tgz && mv * ../$(TAXONOMY)

SOURCES=$(shell echo `find src -name "*.java"`)
STANDALONE=target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar
MEM=-Xmx30g

standalone: $(STANDALONE)

$(STANDALONE): $(SOURCES)
	./compile_standalone.sh -q

taxomachine.db: $(STANDALONE) $(TAXONOMY)/version.txt
	taxonomy_to_graphdb.sh $(TAXONOMY) $@ $(STANDALONE) $(MEM)

tarball: $(TARBALL)
$(TARBALL): taxomachine.db
	echo $(TARBALL)
	tar -czf $@ -C $< .

push-devapi: $(TARBALL)
	scp $< devapi:downloads/$(TARBALL)

# 2. Local testing
# maybe should be NEO=../../neo4j-taxomachine ?

NEO_ROOT=neo4j-community-1.9.5
PLUGIN=target/taxomachine-neo4j-plugins-0.0.1-SNAPSHOT.jar
SETTINGS=host:apihost=http://localhost:7476 host:translate=true
NEOFOO=$(NEO_ROOT)/conf/neo4j-server.properties
NEOTAR=neo4j-community-1.9.5-unix.tar.gz

neo4j: $(NEOFOO)
$(NEOFOO): $(NEOTAR)
	tar xzf $(NEOTAR)
	cp -p $(NEO_ROOT)/conf/neo4j-server.properties $(NEO_ROOT)/conf/neo4j-server.properties.orig
	sed -e s+7474+7476+ -e s+7473+7475+ $(NEO_ROOT)/conf/neo4j-server.properties >sed.tmp
	mv sed.tmp $(NEO_ROOT)/conf/neo4j-server.properties

$(NEOTAR):
	curl http://files.opentreeoflife.org/neo4j/neo4j-community-1.9.5.tar.gz >$(NEOTAR)

push-local: $(TARBALL) $(NEOFOO)
	if [ -e $(NEO_ROOT)/data/graph.db ]; then \
	  rm -rf $(NEO_ROOT)/data/graph.db.previous; \
	  mv -f $(NEO_ROOT)/data/graph.db $(NEO_ROOT)/data/graph.db.previous; fi
	mkdir $(NEO_ROOT)/data/graph.db
	(cd $(NEO_ROOT)/data/graph.db; pwd; tar xzf ../../../$(TARBALL))

compile: $(PLUGIN)

$(PLUGIN): $(SOURCES)
	./compile_server_plugins.sh -q

run: .running

.running: $(PLUGIN)
	$(NEO_ROOT)/bin/neo4j stop
	rm -f .running
	cp -p $(PLUGIN) $(NEO_ROOT)/plugins/
	$(NEO_ROOT)/bin/neo4j start
	touch .running

stop:
	$(NEO_ROOT)/bin/neo4j stop
	rm -f .running

test-v3: .running
	cd ws-tests; \
	for test in test_v3*.py; do \
	  echo $$test; \
	  python $$test $(SETTINGS); \
	done 

test-v2: .running
	cd ws-tests; \
	for test in test_v2*.py; do \
	  echo $$test; \
	  python $$test $(SETTINGS); \
	done

test: test-v2 test-v3

