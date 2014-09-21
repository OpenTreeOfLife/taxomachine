# we are running from within the taxomachine directory
TAXOMACHINE_HOME=pwd

# download and set up ott
OTT_SOURCENAME="aster"
OTT_DOWNLOADDIR=$PREFIX"/data"
mkdir $OTT_DOWNLOADDIR
cd $OTT_DOWNLOADDIR
wget "http://files.opentreeoflife.org/ott/$OTT_SOURCENAME.tgz"
tar -xvf $OTT_SOURCENAME.tgz
OTT_SOURCEDIR="$OTT_DOWNLOADDIR/$OTT_SOURCENAME"
OTT_TAXONOMY=$OTT_SOURCEDIR"/taxonomy.tsv"
OTT_SYNONYMS=$OTT_SOURCEDIR"/synonyms.tsv"
OTT_DEPRECATED=$OTT_SOURCEDIR"/deprecated.tsv"

# pull from the git repo and remove the binary if updating is turned on
TAXOMACHINE_JAR="taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
TAXOMACHINE_COMPILE_LOCATION="$TAXOMACHINE_HOME/target/$TAXOMACHINE_JAR"

# compile taxomachine if necessary
mvn_cmdline.sh
TAXOMACHINE_COMMAND="java -jar $TAXOMACHINE_COMPILE_LOCATION "

# prepare for dealing with the neo4j server
TAXO_NEO4J_HOME="$PREFIX/neo4j-taxomachine"
TAXO_NEO4J_DAEMON="$TAXO_NEO4J_HOME/bin/neo4j"

# download neo4j
wget "http://neo4j.com/artifact.php?name=neo4j-community-1.9.8-unix.tar.gz"
tar -xvf "artifact.php?name=neo4j-community-1.9.8-unix.tar.gz" # this name seems to be used on travis ci server
mv neo4j-community-1.9.8 $TAXO_NEO4J_HOME

# build the ott db
$TAXOMACHINE_COMMAND loadtaxsyn $OTT_SOURCENAME $OTT_TAXONOMY $OTT_SYNONYMS $TAXOMACHINE_DB	
$TAXOMACHINE_COMMAND makecontexts $TAXOMACHINE_DB
$TAXOMACHINE_COMMAND makegenusindexes $TAXOMACHINE_DB
$TAXOMACHINE_COMMAND adddeprecated $OTT_DEPRECATED $TAXOMACHINE_DB
    
# point the server at the taxomachine db location
TAXOMACHINE_DB_ASSIGNMENT="org.neo4j.server.database.location=$TAXOMACHINE_DB"
SERVER_PROPERTIES="$TAXO_NEO4J_HOME/conf/neo4j-server.properties"
ORIG_SERVER_PROPERTIES="$SERVER_PROPERTIES.original"

# edit the text
mv "$SERVER_PROPERTIES" "$ORIG_SERVER_PROPERTIES"
printf "$COMMENT\n$TAXOMACHINE_DB_ASSIGNMENT\n\n" > "$SERVER_PROPERTIES"
grep -v "org.neo4j.server.database.location" "$ORIG_SERVER_PROPERTIES" >> "$SERVER_PROPERTIES"

# install the plugin if necessary
PLUGIN="taxomachine-neo4j-plugins-0.0.1-SNAPSHOT.jar"
PLUGIN_INSTALL_LOCATION="$TAXO_NEO4J_HOME/plugins/"$PLUGIN

# compile the server plugins
mvn_serverplugins.sh
PLUGIN_COMPILE_LOCATION="$TAXOMACHINE_HOME/target/$PLUGIN"
mv "$PLUGIN_COMPILE_LOCATION" "$PLUGIN_INSTALL_LOCATION"

# restart the server
$TAXO_NEO4J_DAEMON restart