# TODO: allow selection of pre-recognized hosts using an argument
#HOST=ec2-54-212-192-235.us-west-2.compute.amazonaws.com # ot2
HOST=ec2-54-212-144-245.us-west-2.compute.amazonaws.com # ot4

# TODO: allow location of identity file to be set via arguments
OPENTREE_IDENTITY=$HOME/opentree.pem

# TODO: allow version and prefix to be set via arguments
VERSION=2.3
PREFIX=~/phylo
SETUP_FLAGS=""

# build the database
if [ ! -f $PREFIX/data/ott$VERSION.tgz ]; then
    SETUP_FLAGS = SETUP_FLAGS" --download-ott"
fi

DB_LOCATION=$PREFIX/data/ott$VERSION.db
if [ ! -d $DB_LOCATION ]; then
    ./setup_taxomachine.sh -prefix $PREFIX $SETUP_FLAGS --setup-db -ott-version $VERSION
fi

# compress the database. important to set the working directory for tar to the database
# directory itself to add its contents to the archive, not the db dir itself
tar -C $DB_LOCATION -czf $DB_LOCATION.tgz .

rsync -e "ssh -i $OPENTREE_IDENTITY" -vax $DB_LOCATION.tgz opentree@$HOST:downloads/taxomachine.db.tgz

# If you put the tarball in the target location as specified above, then the 'install-db.sh'
# installation script can pick it up. Run the installation script as follows:

ssh -i $OPENTREE_IDENTITY opentree@$HOST bash setup/install-db.sh taxomachine
