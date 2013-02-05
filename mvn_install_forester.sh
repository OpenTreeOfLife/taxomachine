#!/bin/sh
TEMP=/tmp/forester_1005.jar
wget -O $TEMP http://forester.googlecode.com/files/forester_1005.jar
mvn install:install-file -Dfile=$TEMP -DgroupId=org.forester -DartifactId=forester -Dversion=1.005 -Dpackaging=jar -DcreateChecksum=true
