wget -O ~/Downloads/forester_1005.jar http://code.google.com/p/forester/downloads/detail?name=forester_1005.jar&can=2&q=
mvn install:install-file -Dfile=$HOME/Downloads/forester_1005.jar -DgroupId=org.forester -DartifactId=forester -Dversion=1.005 -Dpackaging=jar -DcreateChecksum=true
