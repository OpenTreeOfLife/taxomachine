opentree-taxomachine
====================
Taxomachine is a tool for merging, exploring, and exporting taxonomies for use with the Open Tree of Life project. 

Installation
===============
taxomachine is managed by Maven v. 2 (including the dependencies). In order to compile and build treemachine, it is easiest to let Maven v. 2 do the hard work.

On Ubuntu you can install Maven v. 2 with:

	sudo apt-get install maven2

It is also necessary to install the forester library. Directions follow:

Installing forester
--------------
Taxomachine uses the forester library for reading trees, which must be installed into a local repo. To install forester, run the command:

	./mvn_install_forester.sh

This will install forester into a local maven repo inside of the ~/.m2 directory. This only needs to be done once unless you clear out your ~./m2 directory.

Installing Maven 3
--------------

The server plugin must be compiled as a shaded jar, and this functionality now requires the use of Maven 3, which is not yet available via the default Ubuntu apt repositories.

For users of Ubuntu 12.04, Nate Carlson has provided a custom PPA that can be used to greatly simplify the installation process. See the following page for information on this PPA:
https://launchpad.net/~natecarlson/+archive/maven3

To install Maven 3 on Ubuntu 12.04 using this PPA, use the following commands:

	sudo apt-add-repository ppa:natecarlson/maven3
	sudo apt-get update
	sudo apt-get install maven3

For other Ubuntu versions:

...

Compiling
==============

Once Maven v.2 is installed, you can compile the standalone program with:

	sh mvn_cmdline.sh

Compiling the server plugin requires Maven 3. See the above section for more information. Once Maven 3 is installed, you may compile the server plugin with:

	sh mvn_serverplugins.sh

You will then need to place the plugin within the neo4j plugins directory and restart the neo4j server before neo4j will expose its functionality.

Usage
--------------
To see the help message run:

	java -jar taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar

