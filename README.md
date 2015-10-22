[![Build Status](https://secure.travis-ci.org/OpenTreeOfLife/taxomachine.png)](http://travis-ci.org/OpenTreeOfLife/taxomachine)

opentree-taxomachine
====================
Taxomachine is a tool for indexing and querying the Open Tree Taxonomy (OTT) associated with the Open Tree of Life project. It can be run either as a server plugin, or as a standalone program.

The following instructions assume that the working directory is the root of the clone of the taxomachine repository.


Installation
---------------
taxomachine is managed by Maven 3 (including the dependencies). On Ubuntu you can install Maven with:

	sudo apt-get install maven

###Installing OpenTree base packages

Taxomachine depends on [the Open Tree base Java packages](https://github.com/opentreeoflife/ot-base). These must be installed for taxomachine to compile. You can install these manually or by running the command:

```
./install_dependencies.sh
```
	
This adds the ot-base packages to the local maven repository.

Use as a server plugin
--------------

###Compiling

Once Maven is installed and the packages from the ot-base repo have
been successfully added to your local maven repository, you can
compile the taxomachine plugin with:

```
./compile_server_plugins.sh
```

###Installing the server plugin

The server plugins are designed to work with Neo4j version >= 1.9 but < 2.0. To expose the functionality of the server plugin, you must place it into the plugins directory of a compatible Neo4j server, and restart the server. For example:

```
mv target/taxomachine-neo4j-plugins-0.0.1-SNAPSHOT.jar /opt/neo4j-community-1.9.5/plugins/
/opt/neo4j-community-1.9.5/bin/neo4j restart
```

(/opt/neo4j-community-1.9.5 is just a hypothetical location.  Neo4j
can be set up anywhere just by unpacking the distribution file; no
installer or installation step is needed.  On the open tree servers,
the taxomachine neo4j instance is in ~opentree/neo4j-taxomachine.)

To allow inter-domain access to the server plugin (e.g. to call the RESTful services from another domain) it may be necessary to set the Access-Control-Allow-Origin setting on the Neo4j server. Currently, the default setting for this property seems to be no restrictions (Access-Control-Allow-Origin : *) when the server is running with RESTful features. See http://components.neo4j.org/neo4j-server/1.7/apidocs/org/neo4j/server/rest/web/AllowAjaxFilter.html for more information.

To make the Neo4j webserver accept connections from any machine, set the host address to 0.0.0.0 as described at http://docs.neo4j.org/chunked/stable/security-server.html.

Use as a standalone program
--------------

The standalone program can be used to set up the database in advance
of deploying the database with a neo4j instance possessing the
taxomachine plugin.

Again, assuming maven and ot-base are installed as above, compile the standalone program with:

```
./compile_standalone.sh
```

The resulting program (a .jar file) is placed in the target/
directory.  Note that compiling the standalong program will delete any
plugin .jar from the target/ directory and vice versa.

View a help message by running:

```
java -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

System properties
-----------------
Using:

```
java ... -Dopentree.taxomachine.num.transactions=1000 ...  -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar ...
```

in your invocation of taxomachine will sets the maximum # of transactions that will be buffered to 1000.
The default # of transactions is 10000. Higher numbers lead to faster importing of a new taxonomy, 
but using lower numbers will mean that taxomachine will require less memory. (Note, this may no longer be accurate.)

Bootstrapping a database from OTT
----------------------
First, put an OTT version somewhere on the system (here, we use /data/ott, but you can put it anywhere). Second, choose a directory name for the new neo4j database (and make sure the directory does not already exist, else the creation will fail). Here we use graph.db, but you can call it anything.  

Create the database by loading OTT:

```bash
java -Xmx30g -XX:-UseConcMarkSweepGC -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar loadtaxsyn ott /data/ott/taxonomy.tsv /data/ott/synonyms.tsv graph.db
```

For the new database to function for TNRS, it is necessary to build additional indexes. This can be very memory-intensive:

```bash
java -Xmx30g -XX:-UseConcMarkSweepGC -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar makecontexts graph.db
java -Xmx30g -XX:-UseConcMarkSweepGC -jar target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar makegenusindexes graph.db
```

Once the database is ready, it can be moved into a neo4j instance that has the taxomachine plugin:

```bash
/opt/neo4j-community-1.9.5/bin/neo4j stop
rm /opt/neo4j-community-1.9.5/data/graph.db
mv graph.db /opt/neo4j-community-1.9.5/data/graph.db
/opt/neo4j-community-1.9.5/bin/neo4j start
```

You don't need to call it graph.db, but the Open Tree [deployment
scripts](https://github.com/OpenTreeOfLife/opentree/tree/master/deploy) all assume that that is the name of the database directory.

If the neo4j instance is running on a different computer, make a
tarball out of it, transfer it using scp -p, and unpack it on the
other side.  The deployment system has a command 'install-db' for remote unpacking.
The command assumes that the database directory in the tar file is
called 'graph.db'.  (Careful, the following sequence is untested!)

```bash
tar czf taxomachine.db.tgz graph.db
scp -p taxomachine.db.tgz ot10:downloads/new-taxomachine.db.tgz
cd {opentree repo}/deploy
./push.sh -c {config file} install-db downloads/new-taxomachine.db.tgz taxomachine
```
