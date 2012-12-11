package opentree;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

/**
 * TaxonomyLoader is intended to control the initial creation 
 * and addition of taxonomies to the taxonomy graph.
 *
 */
public class TaxonomyLoader extends Taxonomy {

	static Logger _LOG = Logger.getLogger(TaxonomyLoader.class);
	int transaction_iter = 100000;
	int LARGE = 100000000;
	int globaltransactionnum = 0;
	Transaction gtx = null;
	
	static String graphname;
	
	// basic traversal method
	final TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
			.relationships( RelType.TAXCHILDOF,Direction.OUTGOING );
	
	TaxonomyLoader(GraphDatabaseAgent t) {
		super(t);
	}
	
	/**
	 * Returns a node representing the specified combination of taxonomic and nomenclatural status. This may result
	 *	in the addition of a node to the graph (and taxStatusIndex), or it could simply return a previously created
	 *	node from taxStatusIndex
	 * @param taxonomicStatus - 
	 * @param nomenclaturalStatus
	 * @param pendingTx - if not null, the any node created by this operation will *not* be associated with 
	 *	success and finish being called on the transaction object. If pendindTx is null, then a transaction object
	 *	will be created and finalized for any node creation. 
	 * @return 
	 */
	protected Node getNodeForTaxNomStatus(String taxonomicStatus, String nomenclaturalStatus, Transaction pendingTx) {
		String key = taxonomicStatus + "|" + nomenclaturalStatus;
		Index<Node> taxStatus = ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_STATUS);
		IndexHits<Node> ih = taxStatus.get("status", key);
		if (ih.size() == 0) {
			boolean closeTx = false;
			if (pendingTx == null) {
				pendingTx = beginTx();
				closeTx = true;
			}
			//create the metadata node
			Node snode = null;
			try {
				snode = createNode();
				snode.setProperty("taxStatus", taxonomicStatus);
				snode.setProperty("nomenStatus", nomenclaturalStatus);
				taxStatus.add(snode, "status", key);
				if (closeTx) {
					pendingTx.success();
				}
			} finally {
				if (closeTx) {
					pendingTx.finish();
				}
			}
		     ih.close();
			return snode;
		}
		else {
			assert(ih.size() == 1);
			Node n = ih.getSingle();
    	    ih.close();
    	    return n;
		}
	}
	/**
	 * 
	 * @param taxonomicRank
	 * @param pendingTx
	 * @return
	 */
	protected Node getNodeForTaxRank(String taxonomicRank, Transaction pendingTx) {
        Index<Node> taxRanks = ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_STATUS);
        IndexHits<Node> ih = taxRanks.get("rank", taxonomicRank);
		if (ih.size() == 0) {
			boolean closeTx = false;
			if (pendingTx == null) {
				pendingTx = beginTx();
				closeTx = true;
			}
			//create the metadata node
			Node snode = null;
			try {
				snode = createNode();
				snode.setProperty("rank", taxonomicRank);
				taxRanks.add(snode, "rank", taxonomicRank);
				if (closeTx) {
					pendingTx.success();
				}
			} finally {
				if (closeTx) {
					pendingTx.finish();
				}
			}
			ih.close();
			return snode;
		}
		else {
			assert(ih.size() == 1);
			Node n = ih.getSingle();
			ih.close();
			return n;
		}
	}
	
	/**
	 * Reads a Synonomy rows formatted as:
	 * id\t|\tname\t|\tsynonym_type
	 * @param synonymfile filepath of the synonym file
	 * @returns hash with ID as key and the value is an array list of  [name, type] pairs (also as ArrayList objects).
	 */
	private HashMap<String,ArrayList<ArrayList<String>>> readSynonymsFile(String synonymfile) throws FileNotFoundException, IOException {
		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = new HashMap<String,ArrayList<ArrayList<String>>>();
		String str = "";
		BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
		while ((str = sbr.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(str,"\t|\t");
			String id = st.nextToken();
			String name = st.nextToken();
			String type = st.nextToken();
			ArrayList<String> tar = new ArrayList<String>();
			tar.add(name);
			tar.add(type);
			if (synonymhash.get(id) == null) {
				ArrayList<ArrayList<String> > ttar = new ArrayList<ArrayList<String> >();
				synonymhash.put(id, ttar);
			}
			synonymhash.get(id).add(tar);
		}
		sbr.close();
		return synonymhash;
	}
	
	private Node createTaxonomySourceMetadataNode(Properties prop) {
		String sourcename = prop.getProperty("name");
		String sourceversion = prop.getProperty("version");
		Transaction tx = beginTx();
		//create the metadata node
		Node metadatanode = null;
		try {
			metadatanode = createNode();
			metadatanode.setProperty("source", sourcename);
			metadatanode.setProperty("version", sourceversion);
			metadatanode.setProperty("author", "no one");
			ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_SOURCES).add(metadatanode, "source", sourcename);
			tx.success();
		} finally {
			tx.finish();
		}
		return metadatanode;
	}
	
	private void commitNewTaxonomyRelationships(ArrayList<String> temppar, 
			HashMap<String, Node> dbnodes,
			HashMap<String, String> parents, 
			Properties prop) {
		Transaction tx = beginTx();
		String sourcename = prop.getProperty("name");
		try {
			for (int i=0;i<temppar.size();i++) {
				try {
					Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
					rel.setProperty("source", sourcename);
					rel.setProperty("childid",temppar.get(i));
					rel.setProperty("parentid",parents.get(temppar.get(i)));
				} catch(java.lang.IllegalArgumentException io) {
					//System.out.println(temppar.get(i));
					continue;
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	private Node createNewTaxonomyNode(String taxonName) {
		Node tnode = createNode();
		tnode.setProperty("name", taxonName);
		ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME).add(tnode, "name", taxonName);
		return tnode;
	}

	
	/**
	 * Reads a file in the format:
	 * id	| parentID	|	name
	 * and fills dbnodes and parents with the id to Node and id to parent ID lookup info
	 * 
	 * @param filename file to be read
	 * @param synonymfile if not an empty string, this should be a synonym file
	 * @param metadatanode Node in the graph that corresponds to this source
	 * @param dbnodes taxonomic source id to graph node map
	 * @param parents taxonomic source id to parent taxonomic source id
	 * @param prop Properties object that has been initialize with info relevant to this source.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void readTabPipeTabDelimited(String filename, 
								 String synonymfile,
								 Node metadatanode,
								 HashMap<String, Node> dbnodes,
								 HashMap<String, String> parents,
								 Properties prop) throws FileNotFoundException, IOException {

		// key is the id from the taxonomy, the array has the synonym and the type of synonym
		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if (synonymfile.length() > 0)
			synFileExists = true;
		if (synFileExists) {
			synonymhash = readSynonymsFile(synonymfile);
			System.out.println("synonyms: " + synonymhash.size());
		}

		String str = "";
		ArrayList<String> templines = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(filename));
		ArrayList<Node> parentless;
		int count = 0;
		while ((str = br.readLine()) != null) {
			count += 1;
			templines.add(str);
			if (count % transaction_iter == 0) {
				System.out.print(count);
				System.out.print("\n");
				parentless = initCommitLinesToGraph(templines,
													synonymhash,
													dbnodes,
													parents,
													prop);
				for (Node rootNd : parentless) {
					System.out.println("created root node and metadata link");
					metadatanode.createRelationshipTo(rootNd, RelType.METADATAFOR);
				}
				templines.clear();
			}
		}
		br.close();
		parentless = initCommitLinesToGraph(templines,
											synonymhash,
											dbnodes,
											parents,
											prop);
		for (Node rootNd : parentless) {
			System.out.println("created root node and metadata link");
			metadatanode.createRelationshipTo(rootNd, RelType.METADATAFOR);
		}
		templines.clear();
	}

	/**
	 * Reads the GBIF taxonomy
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void readGBif (String archiveDir, 
						  Node metadatanode,
						  HashMap<String, Node> dbnodes,
						  HashMap<String, String> parents,
						  Properties prop) throws FileNotFoundException, IOException {
		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = new HashMap<String,ArrayList<ArrayList<String>>>();
		ArrayList<Node> parentless = new ArrayList<Node>();
		Pattern tabPattern = Pattern.compile("\t");
		BufferedReader br = new BufferedReader(new FileReader(archiveDir));
		int count = 0;
		String str = "";
		Transaction tx = beginTx();
		try {
			while ((str = br.readLine()) != null) {
				// -1 indicates that the array can be any length, and trailing empty matches are *not* discarded
				String [] tokenList = tabPattern.split(str, -1); 
				String acceptedNameUsageIDStr = tokenList[2];
				/*
					System.out.println("\"" + str + "\"");

					System.out.println("length = " + tokenList.length);
					for (int ii = 0; ii < tokenList.length; ++ii) {
						System.out.println("\"" + tokenList[ii] + "\"");
					}
				 */
				if ("".equals(acceptedNameUsageIDStr)) {
					String gbifIDStr = tokenList[0];
					String parentNameUsageIDStr = tokenList[1];
					//String scientificNameStr = tokenList[3];
					//String namePublishedInStr = tokenList[11];
					//String nameAccordingToStr = tokenList[12];

					String taxonRankStr = tokenList[5];
					Node rankNode = getNodeForTaxRank(taxonRankStr, tx);

					String taxonomicStatusStr = tokenList[6];
					String nomenclaturalStatusStr = tokenList[7];
					Node tnStatNode = getNodeForTaxNomStatus(taxonomicStatusStr, nomenclaturalStatusStr, tx);

					String canonicalNameStr = tokenList[4];
					Node tnode = createNewTaxonomyNode(canonicalNameStr);
					dbnodes.put(gbifIDStr, tnode);
					if ("".equals(parentNameUsageIDStr)) {
						parentless.add(tnode);
					}
					else {
						parents.put(gbifIDStr, parentNameUsageIDStr);
					}
					tnode.setProperty("gbif_ID", gbifIDStr);
					tnode.setProperty("gbif_parentNameUsageID", parentNameUsageIDStr);
					//tnode.setProperty("gbif_scientificName", scientificNameStr);
					tnode.setProperty("gbif_canonicalName", canonicalNameStr);
					tnode.setProperty("gbif_taxonRank", rankNode.getId());
					tnode.setProperty("gbif_taxNomStatus", tnStatNode.getId());
					//tnode.setProperty("gbif_namePublishedIn", namePublishedInStr);
					//tnode.setProperty("gbif_nameAccordingTo", nameAccordingToStr);
					/*
						String genusStr = tokenList[8];
						String specificEpithetStr = tokenList[9];
						String infraspecificEpithetStr = tokenList[10];
						String kingdomStr = tokenList[13];
						String phylumStr = tokenList[14];
						String classStr = tokenList[15];
						String orderStr = tokenList[16];
						String familyStr = tokenList[17];
						recordTaxon(tnode, kingdomStr, phylumStr, classStr, orderStr, familyStr, genusStr, specificEpithetStr, infraspecificEpithetStr);
					 */

					count += 1;
					if (count % transaction_iter == 0) {
						tx.success();
						tx.finish();
						tx = beginTx();
						System.err.println("Ingested " + count + " accepted names.");
					}
				}
				else {
					ArrayList<String> tar = new ArrayList<String>(Arrays.asList(tokenList));
					if (synonymhash.get(acceptedNameUsageIDStr) == null) {
						ArrayList<ArrayList<String> > ttar = new ArrayList<ArrayList<String> >();
						synonymhash.put(acceptedNameUsageIDStr, ttar);
					}
					synonymhash.get(acceptedNameUsageIDStr).add(tar);
				}

			}
			tx.success();
			System.err.println("Ingested " + count + " accepted names.");

		} finally {
			tx.finish();
		}
		br.close();
		// At this point, all of the rows without "acceptedNameUsageID" fields will have been ingested
		//	and recorded in dbnodes.
		// All rows with a acceptedNameUsageID field will be stored in synonymhash where the key
		//	is the id of the accepted name
		// We'll add them the synonym nodes for all of the (presumably valid) names here 
		tx = beginTx();
		String sourcename = prop.getProperty("name");
		HashMap<String, Node> synNameToSynNode = new HashMap<String, Node>();
		try {
			count = 0;
			for (String key : dbnodes.keySet()) {
				ArrayList<ArrayList<String>> syns = synonymhash.get(key);
				if (syns != null) {
					Node validNode = dbnodes.get(key);
					for (int j = 0; j < syns.size(); j++) {
						count += 1;
						ArrayList<String> row = syns.get(j);
						createGBIFSynonymNodeHelper(row, validNode, synNameToSynNode, sourcename);
						if (count % transaction_iter == 0) {
							System.err.println("Ingested " + count + " synonyms.");
							tx.success();
							tx.finish();
							tx = beginTx();
						}
					}
					synonymhash.remove(key);
				}
			}
			System.err.println("Ingested " + count + " synonyms.");
			tx.success();
		} finally {
			tx.finish();
		}

		// Now we will add any nodes flagged as that refer to an acceptedNameUsageID, but for which that ID was 
		//	not in dbnodes. These are cases of "indirect" synonyms in the GBIF taxonomy ( A is valid, B->A, and C->B rather than C->A)
		int prevNumIndirect = -1;
		int numIndirect = 0;
		tx = beginTx();
		try {
			while (numIndirect != prevNumIndirect) {
				prevNumIndirect = numIndirect;
				HashSet<String> toDel = new HashSet<String>(); 
				for (String key : synonymhash.keySet()) {
					Node n = synNameToSynNode.get(key); 
					if (n != null) {
						toDel.add(key);
						Node validNode = null;
						for (Relationship rel : n.getRelationships(Direction.OUTGOING, RelType.SYNONYMOF)) {
							assert(validNode == null); // there should only be one outgoing SYNONYMOF relationship...
							validNode = rel.getEndNode();
						}
						assert(validNode != null);

						numIndirect += 1;
						ArrayList<ArrayList<String>> syns = synonymhash.get(key);
						for (int j = 0; j < syns.size(); j++) {
							count += 1;
							ArrayList<String> row = syns.get(j);
							createGBIFSynonymNodeHelper(row, validNode, synNameToSynNode, sourcename);
							System.err.println("Indirect synonym " + row.get(0) + " -> " + validNode.getProperty("name") + "\n");

							if (count % transaction_iter == 0) {
								System.err.println("Ingested " + count + " synonyms.");
								tx.success();
								tx.finish();
								tx = beginTx();
							}
						}
					}
				}
				for (String dk : toDel) {
					synonymhash.remove(dk);
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
		// warn about any synonyms that were not connected to the graph...
		for (String key : synonymhash.keySet()) {
			ArrayList<ArrayList<String>> syns = synonymhash.get(key);
			for (int j = 0; j < syns.size(); j++) {
				ArrayList<String> row = syns.get(j);
				System.err.println("Synonym of disconnected name " + row.get(0) + "\n");
			}
		}

		//
		tx = beginTx();
		try {
			for (Node rootNd : parentless) {
				System.out.println("created root node and metadata link");
				metadatanode.createRelationshipTo(rootNd, RelType.METADATAFOR);
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
	/**
	 * @note Assumes that the caller is wrapping this call in a transaction!
	 * @param row
	 * @param validNode
	 * @param synNameToSynNode
	 * @param sourcename
	 * @return
	 */
	private Node createGBIFSynonymNodeHelper(ArrayList<String> row, Node validNode, HashMap<String, Node> synNameToSynNode, String sourcename) {
		Node synode = createNode();
		String gbifIDStr = row.get(0);
		String canonicalNameStr = row.get(4);
		String taxonomicStatusStr = row.get(6);
		synode.setProperty("name", canonicalNameStr);
		synode.setProperty("nametype", taxonomicStatusStr);
		synode.setProperty("gbif_ID", gbifIDStr);
		synode.setProperty("source", sourcename);
		Relationship rel = synode.createRelationshipTo(validNode, RelType.SYNONYMOF);
		rel.setProperty("source", sourcename);
		synNameToSynNode.put(gbifIDStr, synode);
		return synode;
	}

	private ArrayList<Node> initCommitLinesToGraph(ArrayList<String> templines, 
			HashMap<String,ArrayList<ArrayList<String>>> synonymhash,
			HashMap<String, Node> dbnodes,
			HashMap<String, String> parents,
			Properties prop) {
		boolean synFileExists = synonymhash.isEmpty();
		String sourcename = (String) prop.getProperty("name");
		Transaction tx = beginTx();
		ArrayList<Node> parentless = new ArrayList<Node>();
		try {
			for (int i=0;i<templines.size();i++) {
				StringTokenizer st = new StringTokenizer(templines.get(i),"\t|\t");
				int numtok = st.countTokens();
				String first = st.nextToken();
				String second = "";
				if (numtok == 3)
					second = st.nextToken();
				String third = st.nextToken();
				Node tnode = createNewTaxonomyNode(third);
				dbnodes.put(first, tnode);
				if (numtok == 3) {
					parents.put(first, second);
				} else {//this is the root node
					parentless.add(tnode);
				}
				//synonym processing
				if (synFileExists) {
					if (synonymhash.get(first) != null) {
						ArrayList<ArrayList<String>> syns = synonymhash.get(first);
						for (int j=0; j<syns.size(); j++) {
							Node synode = createNode();
							synode.setProperty("name", syns.get(j).get(0));
							synode.setProperty("nametype", syns.get(j).get(1));
							synode.setProperty("source", sourcename);
							synode.createRelationshipTo(tnode, RelType.SYNONYMOF);
						}
					}
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
		return parentless;
	}
	
	/**
	 * Reads a taxonomy file with rows formatted as:
	 *	taxon_id\t|\tparent_id\t|\tName with spaces allowed\n
	 *
	 * Creates nodes and TAXCHILDOF relationships for each line.
	 * Nodes get a "name" property. Relationships get "source", "childid", "parentid" properties.
	 * 
	 * Nodes are indexed in taxNames "name" key and id value.
	 * 
	 * A metadata node is created to point to the root
	 * 
	 * The line that has no parent will be the root of this tree
	 * 
	 * @param sourcename this becomes the value of a "source" property in every relationship between the taxonomy nodes
	 * @param filename file path to the taxonomy file
	 * @param synonymfile file that holds the synonym
	 */
	public void initializeTaxonomyIntoGraphProperty(String propfilename,
											String filename,
											String synonymfile)
											throws FileNotFoundException, IOException {
		FileInputStream prop_in_stream = new FileInputStream(propfilename);
		Properties prop = new Properties();
		prop.load(prop_in_stream);
		String sourceformat = prop.getProperty("format");
		
		HashMap<String, Node> dbnodes = new HashMap<String, Node>();
		HashMap<String, String> parents = new HashMap<String, String>();
		Node metadatanode = createTaxonomySourceMetadataNode(prop);
		if (sourceformat.equalsIgnoreCase("DWC")) {
			// Darwin core file
			System.err.println("DWC\n");
			/*
			// Example usage of reading DarwinCore from http://gbif.blogspot.com/2009/07/darwin-core-archive-reader-part1.html
			Archive archive = ArchiveFactory.openArchive(new File(filename));
			if (!archive.getCore().hasTerm(DwcTerm.scientificName)) {
			   System.out.println("This application requires dwc-a with scientific names");
			   System.exit(1);
			}
			Iterator<DarwinCoreRecord> a_iter = archive.iteratorDwc();
			DarwinCoreRecord dwc;
			long x = 1;
			while (a_iter.hasNext()) {
				dwc = a_iter.next();
				if (null != dwc.id()) {
					System.out.println("ID = " + dwc.id() + " ParentID = " + dwc.getParentNameUsageID() + " canonicalName = " + dwc.getProperty(GbifTerm.canonicalName) + " getTaxonAttributes = " + dwc.getTaxonAttributes());
					x += 1;
					if (x > 1000) {
						break;
					}
				}
			}
			System.err.println("x= " + x + "\n");
			*/
			readGBif (filename, metadatanode, dbnodes, parents, prop);
		} else {
			readTabPipeTabDelimited(filename, synonymfile, metadatanode, dbnodes, parents, prop);
		}
		
		//add the relationships based on dbnodes and parents
		ArrayList<String> temppar = new ArrayList<String>();
		int count = 0;
		for (String key: dbnodes.keySet()) {
			count += 1;
			temppar.add(key);
			if (count % transaction_iter == 0) {
				System.out.println(count);
				commitNewTaxonomyRelationships(temppar, dbnodes, parents, prop);
				temppar.clear();
			}
		}
		commitNewTaxonomyRelationships(temppar, dbnodes, parents, prop);
	}

	/**
	 * Reads a taxonomy file with rows formatted as:
	 *	taxon_id\t|\tparent_id\t|\tName with spaces allowed\n
	 *
	 * Creates nodes and TAXCHILDOF relationships for each line.
	 * Nodes get a "name" property. Relationships get "source", "childid", "parentid" properties.
	 * 
	 * Nodes are indexed in taxNames "name" key and id value.
	 * 
	 * A metadata node is created to point to the root
	 * 
	 * The line that has no parent will be the root of this tree
	 * 
	 * @param sourcename this becomes the value of a "source" property in every relationship between the taxonomy nodes
	 * @param filename file path to the taxonomy file
	 * @param synonymfile file that holds the synonym
	 */
	public void initializeTaxonomyIntoGraphName(String sourcename, String filename, String synonymfile) {
		String str = "";
		int count = 0;
		Transaction tx;
		ArrayList<String> templines = new ArrayList<String>();
		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if (synonymfile.length() > 0)
			synFileExists = true;
		//preprocess the synonym file
		//key is the id from the taxonomy, the array has the synonym and the type of synonym
		if (synFileExists) {
			synonymhash = new HashMap<String,ArrayList<ArrayList<String>>>();
			try {
				BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
				while ((str = sbr.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(str,"\t|\t");
					String id = st.nextToken();
					String name = st.nextToken();
					String type = st.nextToken();
					ArrayList<String> tar = new ArrayList<String>();
					tar.add(name);tar.add(type);
					if (synonymhash.get(id) == null) {
						ArrayList<ArrayList<String> > ttar = new ArrayList<ArrayList<String> >();
						synonymhash.put(id, ttar);
					}
					synonymhash.get(id).add(tar);
				}
				sbr.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			System.out.println("synonyms: " + synonymhash.size());
		}
		//finished processing synonym file

		HashMap<String, Node> dbnodes = new HashMap<String, Node>();
		HashMap<String, String> parents = new HashMap<String, String>();

		Index<Node> taxSources = ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_SOURCES);
		Index<Node> taxaByName = ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
		Index<Node> taxaBySynonym = ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_SYNONYM);
		
		try {
			tx = beginTx();
			//create the metadata node
			Node metadatanode = null;
			try {
				metadatanode = createNode();
				metadatanode.setProperty("source", sourcename);
				metadatanode.setProperty("author", "no one");
				//taxSourceIndex.add(metadatanode, "source", sourcename);
				taxSources.add(metadatanode, "source", sourcename);
				tx.success();
			} finally {
				tx.finish();
			}
			BufferedReader br = new BufferedReader(new FileReader(filename));
			while ((str = br.readLine()) != null) {
				count += 1;
				templines.add(str);
				if (count % transaction_iter == 0) {
					System.out.print(count);
					System.out.print("\n");
					tx = beginTx();
					try {
						for (int i=0; i<templines.size(); i++) {
							StringTokenizer st = new StringTokenizer(templines.get(i),"\t|\t");
							int numtok = st.countTokens();
							String inputId = st.nextToken();
							String InputParentId = "";
							if (numtok == 3)
								InputParentId = st.nextToken();
							String inputName = st.nextToken();
							Node tnode = createNode();
							tnode.setProperty("name", inputName);
							tnode.setProperty("uid", "");
							//taxNodeIndex.add( tnode, "name", inputName);
							taxaByName.add(tnode, "name", inputName);
							dbnodes.put(inputId, tnode);
							if (numtok == 3) {
								parents.put(inputId, InputParentId);
							} else { // this is the root node
								System.out.println("created root node and metadata link");
								metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
							}
							// synonym processing
							if (synFileExists) {
								if (synonymhash.get(inputId) != null) {
									ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
									for (int j=0; j < syns.size(); j++) {
										
										String synName = syns.get(j).get(0);
										String synNameType = syns.get(j).get(1);
										
										Node synode = createNode();
										synode.setProperty("name",synName);
										synode.setProperty("uid", "");
										synode.setProperty("nametype",synNameType);
										synode.setProperty("source",sourcename);
										synode.createRelationshipTo(tnode, RelType.SYNONYMOF);
										taxaBySynonym.add(tnode, "name", synName);
									}
								}
							}
						}
						tx.success();
					} finally {
						tx.finish();
					}
					templines.clear();
				}
			}
			br.close();
			tx = beginTx();
			try {
				for (int i=0; i < templines.size(); i++) {
					StringTokenizer st = new StringTokenizer(templines.get(i),"\t|\t");
					int numtok = st.countTokens();
					String first = st.nextToken();
					String second = "";
					if (numtok == 3)
						second = st.nextToken();
					String third = st.nextToken();
					Node tnode = createNode();
					tnode.setProperty("name", third);
					tnode.setProperty("uid", "");
					//taxNodeIndex.add( tnode, "name", third);
					taxaByName.add( tnode, "name", third);
					dbnodes.put(first, tnode);
					if (numtok == 3) {
						parents.put(first, second);
					} else {//this is the root node
						System.out.println("created root node and metadata link");
						metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
					}
					//synonym processing
					if (synFileExists) {
						if (synonymhash.get(first) != null) {
							ArrayList<ArrayList<String>> syns = synonymhash.get(first);
							for (int j=0; j < syns.size(); j++) {
								
								String synName = syns.get(j).get(0);
								String synNameType = syns.get(j).get(1);
								
								Node synode = createNode();
								synode.setProperty("name",synName);
								synode.setProperty("nametype",synNameType);
								synode.setProperty("source",sourcename);
								synode.setProperty("uid", "");
								synode.createRelationshipTo(tnode, RelType.SYNONYMOF);
								taxaBySynonym.add(tnode, "name", synName);
							}
						}
					}
				}
				tx.success();
			} finally {
				tx.finish();
			}
			templines.clear();
			//add the relationships
			ArrayList<String> temppar = new ArrayList<String>();
			count = 0;
			for (String key: dbnodes.keySet()) {
				count += 1;
				temppar.add(key);
				if (count % transaction_iter == 0) {
					System.out.println(count);
					tx = beginTx();
					try {
						for (int i=0; i < temppar.size(); i++) {
							try {
								Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
								rel.setProperty("source", sourcename);
								rel.setProperty("childid",temppar.get(i));
								rel.setProperty("parentid",parents.get(temppar.get(i)));
							} catch(java.lang.IllegalArgumentException io) {
//								System.out.println(temppar.get(i));
								continue;
							}
						}
						tx.success();
					} finally {
						tx.finish();
					}
					temppar.clear();
				}
			}
			tx = beginTx();
			try {
				for (int i=0; i < temppar.size(); i++) {
					try {
						Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
						rel.setProperty("source", sourcename);
						rel.setProperty("childid",temppar.get(i));
						rel.setProperty("parentid",parents.get(temppar.get(i)));
					} catch(java.lang.IllegalArgumentException io) {
//						System.out.println(temppar.get(i));
						continue;
					}
				}
				tx.success();
			} finally {
				tx.finish();
			}
		} catch(IOException ioe) {}
		//mark all of the barrier nodes with additional metadata
		System.out.println("setting barrier nodes");
		BarrierNodes bn = new BarrierNodes(this);
		ArrayList<Node> barrierNodes = bn.getBarrierNodes();
		HashMap<String,String> barrierNodesMap = (HashMap<String,String>)bn.getBarrierNodeMap();
		TraversalDescription CHILDREN_TRAVERSAL = Traversal.description()
				.relationships( RelType.TAXCHILDOF,Direction.INCOMING );
		tx = beginTx();
		try {
			for (int i=0; i < barrierNodes.size(); i++) {
				for (Node currentNode : CHILDREN_TRAVERSAL.traverse(barrierNodes.get(i)).nodes()) {
					currentNode.setProperty("taxcode", barrierNodesMap.get(barrierNodes.get(i).getProperty("name")));
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	HashMap<String, ArrayList<String>> globalchildren = null;
	HashMap<String,String> globalidnamemap = null;
	PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelType.TAXCHILDOF, Direction.OUTGOING ),10000);
	HashMap<Node,Node> lastexistingmatchparents = new HashMap<Node,Node>();
	
	/**
	 * See addInitialTaxonomyTableIntoGraph 
	 * This function acts like addInitialTaxonomyTableIntoGraph but it 
	 *	can be called for a taxonomy that is not the first taxonomy in the graph
	 * 
	 * Rather than each line resulting in a new node, only names that have not
	 *		 been encountered before will result in new node objects.
	 *
	 * To connect a subtree from the new taxonomy to the taxonomy tree the 
	 *	taxNodeIndex of the existing graph is checked the new name. If multiple
	 *	nodes have been assigned the name, then the one with the lowest score
	 *	is assumed to be the closest match (the score is calculated by counting
	 *	the number of nodes traversed in the path new->anc* + the number of 
	 *	nodes in old->anc* where "anc*" denotes the lowest ancestor in
	 *	the new taxon's ancestor path that has a match in the old graph (and
	 *	the TAXCHILDOF is the relationship on the path).
	 *	
	 * @param filename file path to the taxonomy file
	 * @param sourcename this becomes the value of a "source" property in every relationship between the taxonomy nodes
	 */
	public void addAdditionalTaxonomyToGraph(String sourcename, String rootid, String filename, String synonymfile) {
	    System.out.println("received: " + sourcename + " " + rootid + " " + filename + " " + synonymfile);
		Node rootnode = null;
		String str = "";
		String roottaxid = "";
		if (rootid.length() > 0) {
			rootnode = getNodeById(Long.valueOf(rootid));
			System.out.println(rootnode);
		}
		PathFinder<Path> tfinder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelType.TAXCHILDOF, Direction.OUTGOING ), 10000);

		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if (synonymfile.length() > 0)
			synFileExists = true;
		//preprocess the synonym file
		//key is the id from the taxonomy, the array has the synonym and the type of synonym
		if (synFileExists) {
			synonymhash = new HashMap<String,ArrayList<ArrayList<String>>>();
			try {
				BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
				while ((str = sbr.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(str,"\t|\t");
					String id = st.nextToken();
					String name = st.nextToken();
					String type = st.nextToken();
					ArrayList<String> tar = new ArrayList<String>();
					tar.add(name);tar.add(type);
					if (synonymhash.get(id) == null) {
						ArrayList<ArrayList<String> > ttar = new ArrayList<ArrayList<String> >();
						synonymhash.put(id, ttar);
					}
					synonymhash.get(id).add(tar);
				}
				sbr.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			System.out.println("synonyms: "+synonymhash.size());
		}
		
		//get what barriers in taxonomy are parent to the input root (so is this
		//higher than plants, fungi, or animals (helps clarify homonyms
		BarrierNodes bn = new BarrierNodes(this);
		ArrayList<Node> barrierNodes = bn.getBarrierNodes();
		//HashMap<String,String> barrierNodesMap = (HashMap<String,String>)bn.getBarrierNodeMap(); // not used
		Node rootbarrier = null;
		for (int i=0; i < barrierNodes.size(); i++) {
			Path tpath = tfinder.findSinglePath(rootnode, barrierNodes.get(i));
			if (tpath != null)
				rootbarrier = barrierNodes.get(i);
		}
		HashMap<String,Node> taxcontainedbarriersmap = new HashMap<String,Node>();
		
		Index<Node> taxSources = ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_SOURCES);
		
		HashMap<String, String> idparentmap = new HashMap<String, String>(); // node number -> parent's number
		HashMap<String, String> idnamemap = new HashMap<String, String>();
		ArrayList<String> idlist = new ArrayList<String>();
		Transaction tx;
		//GET NODE
		tx = beginTx();
		try {
			Node metadatanode = null;
			metadatanode = createNode();
			metadatanode.setProperty("source", sourcename);
			metadatanode.setProperty("author", "no one");
			taxSources.add(metadatanode, "source", sourcename);
			try {
				BufferedReader br = new BufferedReader(new FileReader(filename));
				while ((str = br.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(str,"\t|\t");
					int numtok = st.countTokens();
					String id = st.nextToken();
					idlist.add(id);
					String parid = "";
					if (numtok == 3)
						parid = st.nextToken();
					String name = st.nextToken();
					idnamemap.put(id, name);
					if (numtok == 3) {
						idparentmap.put(id, parid);
					} else {//this is the root node
						if (rootnode == null) {
							System.out.println("the root should never be null");
							System.exit(0);
							//if the root node is null then you need to make a new one
							//rootnode = graphDb.createNode();
							//rootnode.setProperty("name", third);
						}
						roottaxid = id;
						System.out.println("matched root node and metadata link");
						metadatanode.createRelationshipTo(rootnode, RelType.METADATAFOR);
					}
					//check to see if the name is a barrier
					HashSet<String> barrierNames = (HashSet<String>) bn.getBarrierNodeNames();
					if (barrierNames.contains(name)) {
						for (int j=0; j < barrierNodes.size(); j++) {
							if (((String)barrierNodes.get(j).getProperty("name")).equals(name)) {	
								taxcontainedbarriersmap.put(id,barrierNodes.get(j));
								System.out.println("added barrier node "+name);
							}
						}
					}
				}
				br.close();
			} catch(Exception e) {
				e.printStackTrace();
				System.out.println("problem with infile");
				System.exit(0);
			}
			tx.success();
		} finally {
			tx.finish();
		}
	
		
		Index<Node> taxaByName = ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
		
		System.out.println("node run through");
		HashMap<String,Node> idnodemap = new HashMap<String,Node>();
		int count = 0;
		int acount = 0;
		tx=beginTx();
		try {
			for (int i=0; i < idlist.size(); i++) {
				String curid = idlist.get(i);
//				System.out.println("attempting id " + String.valueOf(curid));
				boolean badpath = false;
				Node curidbarrier = null;
				ArrayList<String> path1 = new ArrayList<String>();
				while (curid.equals(roottaxid) == false) {
					if (idparentmap.containsKey(curid) == false) {
						badpath = true;
						break;
					} else {
						if (taxcontainedbarriersmap.containsKey(curid))
							curidbarrier = taxcontainedbarriersmap.get(curid);
						curid = idparentmap.get(curid);
						path1.add(curid);
					}
				}
				curid = idlist.get(i);
				if (curidbarrier == null) {
					curidbarrier = rootbarrier;
				}
//							System.out.print(curidbarrier);
				if (badpath) {
					System.out.println("bad path: " + idlist.get(i) + " " + idnamemap.get(curid));
					continue;
				}
				//get any hits
				IndexHits<Node> hits = taxaByName.get("name", idnamemap.get(curid));
//				System.out.println("found " + hits.size() + " hits for " + idnamemap.get(curid));
				try {
					if (hits.size()==0) {//no hit
						Node newnode = createNode();
						newnode.setProperty("name", idnamemap.get(curid));
						newnode.setProperty("uid", "");
						if (curidbarrier != null) {
							if (curidbarrier.hasProperty("taxcode"))
								newnode.setProperty("taxcode", (String)curidbarrier.getProperty("taxcode"));
						}
						taxaByName.add( newnode, "name", idnamemap.get(curid));
						idnodemap.put(curid,newnode);
						acount += 1;
//						System.out.println("new name: " + idnamemap.get(curid));
					} else {
						Node bestnode = null;
						for (Node node : hits) {
							if (node.hasProperty("taxcode")) {
								if (curidbarrier != null) {
									if (curidbarrier.hasProperty("taxcode"))
										if (node.getProperty("taxcode").equals(curidbarrier.getProperty("taxcode")))
											bestnode = node;
								} else {
//									System.out.println("name: " + idnamemap.get(curid) + " " + ((String)node.getProperty("taxcode")));
									//these are often problems
								}
							} else {
								if (node.hasProperty("taxcode") == false)
									if (curidbarrier != null) {
										if (curidbarrier.hasProperty("taxcode") == false)
											bestnode = node;
									} else {
										bestnode = node;
									}
							}
						}
						if (bestnode == null) {
							Node newnode = createNode();
							newnode.setProperty("name", idnamemap.get(curid));
							newnode.setProperty("uid", "");
							if (curidbarrier != null) {
								if (curidbarrier.hasProperty("taxcode"))
									newnode.setProperty("taxcode", (String)curidbarrier.getProperty("taxcode"));
							}
							taxaByName.add( newnode, "name", idnamemap.get(curid));
							idnodemap.put(curid,newnode);
							System.out.println("node to make " + idnamemap.get(curid) + " " + curidbarrier + " " + curid);
							acount += 1;
						} else {
							idnodemap.put(curid,bestnode);
						}
					}
				} finally {
					hits.close();
				}
				count += 1;
				if (count % 10000 == 0)
					System.out.println(count + " " + acount);
			}
			tx.success();
		} finally {
			tx.finish();
		}

		System.out.println("relationship run through");
		tx=beginTx();
		try {
			count = 0;
			for (int i=0; i < idlist.size(); i++) {
				String curid = idlist.get(i);
				if (idparentmap.containsKey(curid)) {//not the root
					//get the node
					//make the relationship
					Node tnode = idnodemap.get(curid);
					Node tnodep = idnodemap.get(idparentmap.get(curid));
					Relationship rel = tnode.createRelationshipTo(tnodep, RelType.TAXCHILDOF);
					rel.setProperty("source", sourcename);
					rel.setProperty("childid", curid);
					rel.setProperty("parentid", idparentmap.get(curid));
					count += 1;
					if (count % 100000 == 0)
						System.out.println(count);
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
		//synonym processing
		tx=beginTx();
		try {
			if (synFileExists) {
				System.out.println("synonyms processing");
				for (int i=0; i < idlist.size(); i++) {
					String first = idlist.get(i);
					if (synonymhash.get(first) != null) {
						Node tnode = idnodemap.get(first);
						ArrayList<ArrayList<String>> syns = synonymhash.get(first);
						for (int j=0; j < syns.size(); j++) {
							String synName = syns.get(j).get(0);
							String synNameType = syns.get(j).get(1);
							Node synode = createNode();
							synode.setProperty("name",synName);
							synode.setProperty("nametype",synNameType);
							synode.setProperty("source",sourcename);
							synode.setProperty("uid", "");
							synode.createRelationshipTo(tnode, RelType.SYNONYMOF);
							ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_SYNONYM).add(tnode, "name", synName);
						}
					}
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	public void runittest(String filename,String filename2) {

	}
	
	public static void main( String[] args ) {
		System.out.println( "unit testing taxonomy loader" );
		final String DB_PATH = "/home/smitty/Dropbox/projects/AVATOL/graphtests/neo4j-community-1.8.M02/data/graph.db";
		GraphDatabaseAgent taxdb = new GraphDatabaseAgent(DB_PATH);
		TaxonomyLoader a = new TaxonomyLoader(taxdb);
		//String filename = "/media/data/Dropbox/projects/AVATOL/graphtests/taxonomies/union4.txt";
		String filename =  "/home/smitty/Dropbox/projects/AVATOL/graphtests/taxonomies/col_acc.txt";
		String filename2 = "/home/smitty/Dropbox/projects/AVATOL/graphtests/taxonomies/ncbi_no_env_samples.txt";
		a.runittest(filename,filename2);
		System.exit(0);
	}
}
