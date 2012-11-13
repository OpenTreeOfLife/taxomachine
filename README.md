opentree-taxomachine
====================

taxonomy graphdb

===============
Installation
---------------
taxomachine is managed by Maven v. 2 (including the dependencies). In order to compile and build treemachine, it is easiest to let Maven v. 2 do the hard work.

On Ubuntu you can install Maven v. 2 with:
sudo apt-get install maven2

Once Maven v. 2 is installed, you can compile with:
	
	sh mvn_cmdline.sh

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

Usage
--------------
To see the help message run:

	java -jar taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar

