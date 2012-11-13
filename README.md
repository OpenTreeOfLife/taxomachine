opentree-taxomachine
====================
Taxomachine is a tool for merging, exploring, and exporting taxonomies for use with the Open Tree of Life project. 

Installation
---------------
taxomachine is managed by Maven v. 3 (including the dependencies). In order to compile and build treemachine, it is easiest to let Maven v. 2 do the hard work.

On Ubuntu you can install Maven with:

	sudo apt-get install maven

It is also necessary to install the forester library, which must be installed into a local repo. To install forester, run the script:

	sh mvn_install_forester.sh

This will install forester into a local maven repo inside of the ~/.m2 directory. This only needs to be done once unless you clear out your ~./m2 directory.

Compiling
--------------

Once Maven is installed and the local forester repo has been successfully added, you can compile the standalone program with:

	sh mvn_cmdline.sh

Or the server plugin with:

	sh mvn_serverplugins.sh

Installing the server plugin
--------------

To expose the functionality of the server plugin, you must place it into your neo4j plugins directory, and restart the neo4j server. For example:

	mv target/opentree-neo4j-plugins-0.0.1-SNAPSHOT.jar /opt/neo4j-community-1.8/plugins
	/opt/neo4j-community-1.8/bin/neo4j restart

Usage
--------------
For the standalone version, view the help message by running:

	java -jar taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar

