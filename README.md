[![Build Status](https://secure.travis-ci.org/OpenTreeOfLife/taxomachine.png)](http://travis-ci.org/OpenTreeOfLife/taxomachine)

opentree-taxomachine
====================
Taxomachine is a tool for indexing and querying the Open Tree Taxonomy (OTT) associated with the Open Tree of Life project.


Installation
---------------
taxomachine is managed by Maven 3 (including the dependencies). On Ubuntu you can install Maven with:

	sudo apt-get install maven

###Installing OpenTree base packages

Taxomachine depends on [the Open Tree base Java packages](https://github.com/opentreeoflife/ot-base). These must be installed for taxomachine to compile. You can install these manually (see the above link) or by running the command:

```
./install_dependencies.sh
```
	
Compiling
--------------

Once Maven is installed and the packages from the ot-base repo have been successfully added to your local maven repository, you can compile the standalone taxomachine program with:

```
./compile_standalone.sh
```

Or the server plugins with:

```
./compile_server_plugins.sh
```

Installing the server plugin
--------------

The server plugins are designed to work with Neo4j version >= 1.9 but < 2.0. To expose the functionality of the server plugin, you must place it into the plugins directory of a compatible Neo4j server, and restart the server. For example:

```
mv target/taxomachine-neo4j-plugins-0.0.1-SNAPSHOT.jar /opt/neo4j-community-1.9.5/plugins
/opt/neo4j-community-1.9.5/bin/neo4j restart
```

To allow inter-domain access to the server plugin (e.g. to call the RESTful services from another domain) it may be necessary to set the Access-Control-Allow-Origin setting on the Neo4j server. Currently, the default setting for this property seems to be no restrictions (Access-Control-Allow-Origin : *) when the server is running with RESTful features. See http://components.neo4j.org/neo4j-server/1.7/apidocs/org/neo4j/server/rest/web/AllowAjaxFilter.html for more information.

To make the Neo4j webserver accept connections from any machine, set the host address to 0.0.0.0 as described at http://docs.neo4j.org/chunked/stable/security-server.html.

Usage
--------------
For the standalone version, view the help message by running:

```
java -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

Bootstrapping from OTT
----------------------
If you have ott2.0 store in /data/ott2.0
and you want to create a new db at taxomachine.db
then you can use:

```bash
java -Xmx10g -XX:-UseConcMarkSweepGC -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar loadtaxsyn ott /data/ott2.0/taxonomy /data/ott2.0/synonyms taxomachine.db
```

For the new database to function for TNRS, it is necessary to build additional indexes. This can be very memory-intensive:

```bash
java -Xmx30g -XX:-UseConcMarkSweepGC -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar makecontexts taxomachine.db
java -Xmx30g -XX:-UseConcMarkSweepGC -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar makegenusindexes taxomachine.db
```

System Properties
-----------------
Using:

```
java ... -Dopentree.taxomachine.num.transactions=1000 ...  -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar ...
```

in your invocation of taxomachine will sets the maximum # of transactions that will be buffered to 1000.
The default # of transactions is 10000. Higher numbers lead to faster importing of a new taxonomy, 
but using lower numbers will mean that taxomachine will require less memory. (Note, this may no longer be accurate.)
