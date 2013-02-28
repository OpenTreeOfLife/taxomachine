java -jar ../target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar inittax ncbi Dip.ncbi dip.db
java -jar ../target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar addtax gbif Dip.gbif dip.db
java -jar ../target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar graftbycomp dip.db gbif
