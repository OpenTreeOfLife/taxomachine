cd ../ && ./mvn_cmdline.sh
cd example
java -jar ../target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar inittax ncbi nematoda.ncbi nem.db
java -jar ../target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar addtax gbif nematoda.gbif nem.db
java -jar ../target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar graftbycomp nem.db gbif
java -jar ../target/taxomachine-0.0.1-SNAPSHOT-jar-with-dependencies.jar makeottol nem.db
