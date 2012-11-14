wget -O ~/Downloads/forester_1005.jar http://forester.googlecode.com/files/forester_1005.jar
mvn install:install-file -Dfile=$HOME/Downloads/forester_1005.jar -DgroupId=org.forester -DartifactId=forester -Dversion=1.005 -Dpackaging=jar -DcreateChecksum=true
