# we are running from within the taxomachine directory
TAXOMACHINE_HOME=pwd

# download and set up ott
wget "http://files.opentreeoflife.org/ott/aster.tgz"
tar -xvf aster.tgz
OTT_TAXONOMY="aster/taxonomy.tsv"
OTT_SYNONYMS="aster/synonyms.tsv"
OTT_DEPRECATED="aster/deprecated.tsv"

# pull from the git repo and remove the binary if updating is turned on
TAXOMACHINE_JAR="target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

# compile taxomachine if necessary
mvn_cmdline.sh
TAXOMACHINE_COMMAND="java -jar $TAXOMACHINE_JAR "

# download neo4j
wget "http://neo4j.com/artifact.php?name=neo4j-community-1.9.8-unix.tar.gz"
tar -xvf "artifact.php?name=neo4j-community-1.9.8-unix.tar.gz" # this name seems to be used on travis ci server
mv neo4j-community-1.9.8 neo4j-server

# build the ott db
$TAXOMACHINE_COMMAND loadtaxsyn $OTT_SOURCENAME $OTT_TAXONOMY $OTT_SYNONYMS $TAXOMACHINE_DB	
$TAXOMACHINE_COMMAND makecontexts $TAXOMACHINE_DB
$TAXOMACHINE_COMMAND makegenusindexes $TAXOMACHINE_DB
$TAXOMACHINE_COMMAND adddeprecated $OTT_DEPRECATED $TAXOMACHINE_DB
    
# point the server at the taxomachine db location
TAXOMACHINE_DB_ASSIGNMENT="org.neo4j.server.database.location=$TAXOMACHINE_DB"
SERVER_PROPERTIES="neo4j-server/conf/neo4j-server.properties"
ORIG_SERVER_PROPERTIES="$SERVER_PROPERTIES.original"
mv "$SERVER_PROPERTIES" "$ORIG_SERVER_PROPERTIES"
printf "$TAXOMACHINE_DB_ASSIGNMENT\n\n" > "$SERVER_PROPERTIES"
grep -v "org.neo4j.server.database.location" "$ORIG_SERVER_PROPERTIES" >> "$SERVER_PROPERTIES"

# compile and install the server plugin
mvn_serverplugins.sh
PLUGIN_COMPILE_LOCATION="target/taxomachine-neo4j-plugins-0.0.1-SNAPSHOT.jar"
PLUGIN_INSTALL_LOCATION="neo4j-server/plugins/taxomachine-plugin.jar"
mv "$PLUGIN_COMPILE_LOCATION" "$PLUGIN_INSTALL_LOCATION"

# restart the server
neo4j-server/bin/neo4j restart