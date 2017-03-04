#!/bin/bash

set -e

# Convert a smasher taxonomy file set into a neo4j graphdb suitable
# for use with taxomachine.
# Hmm, taxonomy path should not end with /

if [ $# -lt 2 ]; then
  echo "Not enough arguments"
  exit 1
fi

TAXONOMY=$1
GRAPHDB=$2
STANDALONE=target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar
MEM=

[ $# -gt 3 ] && STANDALONE=$3
[ $# -gt 4 ] && MEM=$4

# Flush previous version of graphdb
if [ -e $GRAPHDB ]; then
  rm -rf $GRAPHDB.previous
  mv -f $GRAPHDB $GRAPHDB.previous
fi

TV=`basename $TAXONOMY`
[ -e $TAXONOMY/version.txt ] && TV=$TV`cat $TAXONOMY/version.txt`

JAVA="java $MEM -XX:-UseConcMarkSweepGC -jar $STANDALONE"

# Build new graphdb from scratch
$JAVA loadtaxsyn $TV $TAXONOMY/taxonomy.tsv $TAXONOMY/synonyms.tsv $TAXONOMY/forwards.tsv $GRAPHDB
$JAVA makecontexts $GRAPHDB
$JAVA makegenusindexes $GRAPHDB
