package opentree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

/**
 * TaxonomyLoader is intended to control the initial creation 
 * and addition of taxonomies to the taxonomy graph.
 *
 * TaxonomyLoader major functions
 * ==========================
 * initializeTaxonomyIntoGraph (initialize a taxonomy, in our case NCBI, into the graph
 *                              the significance of this is that this will be in the main
 *                              index and will serve as the backbone for the taxonomy 
 *                              synthesis. Added to the index as
 *                              ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME)
 *                                 as (tnode, "name", inputName))
 * 
 * addDisconnectedTaxonomyToGraph (add a taxonomy into the graph. this will not be connected
 *                                 to the main taxonomy and in order to incorporate it into
 *                                 the main taxonomy must be compared in the TaxonomyComparator.
 *                                 Also, this will be added to 
 *                                 ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME)
 *                                 as (tnode, sourcename, inputName))
 * 
 * BOTH OF THESE MUST HAVE A LIFE NODE
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
		} else {
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
		} else {
			assert(ih.size() == 1);
			Node n = ih.getSingle();
			ih.close();
			return n;
		}
	}
	



	/**
	 * Reads a taxonomy file with rows formatted as:
	 *	taxon_id\t|\tparent_id\t|\tName\t|\trank with spaces allowed\n
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
	public void initializeTaxonomyIntoGraph(String sourcename, String filename, String synonymfile) {
		String str = "";
		int count = 0;
		Transaction tx;
		ArrayList<String> templines = new ArrayList<String>();
		HashMap<String, ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if (synonymfile.length() > 0) {
			synFileExists = true;
		}
		//preprocess the synonym file
		//key is the id from the taxonomy, the array has the synonym and the type of synonym
		if (synFileExists) {
			synonymhash = new HashMap<String, ArrayList<ArrayList<String>>>();
			try {
				BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
				while ((str = sbr.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(str, "\t|\t");
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

		// just setting source metadata manually for now
		String author = "";
		String weburl = "";
		String uri = "";
		String urlPrefix = "";
		if (sourcename == "ncbi") {
            author = "no one";
            weburl = "http://ncbi.nlm.nih.gov";
            uri = "";
            urlPrefix = "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=";
		} else if (sourcename == "gbif") {
            author = "gbif";
            weburl = "http://www.gbif.org/";
            uri = "";
            urlPrefix = "http://ecat-dev.gbif.org/usage/";		    
		}
		
		Node metadatanode = null;
		try {
			tx = beginTx();
			//create the metadata node
			try {
				metadatanode = createNode();
				metadatanode.setProperty("source", sourcename);
				metadatanode.setProperty("author", author);
				metadatanode.setProperty("weburl", weburl);
				metadatanode.setProperty("uri", uri);
				metadatanode.setProperty("urlprefix", urlPrefix);
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
						for (int i = 0; i < templines.size(); i++) {
							StringTokenizer st = new StringTokenizer(templines.get(i), "\t|\t");
							int numtok = st.countTokens();
							String inputId = st.nextToken();
							String nexttok = st.nextToken();
							String inputParentId = "";
							String inputName = "";
							String rank = "";
							Node tnode = createNode();
							//if it equals life it won't have a parent
							if (nexttok.equals("life") == false) {
								inputParentId = nexttok;
								inputName = st.nextToken();
								if (numtok == 4) {
									tnode.setProperty("rank", st.nextToken());
								}
								parents.put(inputId, inputParentId);
							} else {//root
								inputName = nexttok;
								System.out.println("created root node and metadata link");
								metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
							}
							tnode.setProperty("name", inputName);
							//TODO: add the ability to input these from a source if they have already been set
							tnode.setProperty("uid", tnode.getId());
							tnode.setProperty("sourceid", inputId);
							tnode.setProperty("sourcepid", inputParentId);
							tnode.setProperty("source", sourcename);
							taxaByName.add(tnode, "name", inputName);
							dbnodes.put(inputId, tnode);
							// synonym processing
							if (synFileExists) {
								if (synonymhash.get(inputId) != null) {
									ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
									for (int j = 0; j < syns.size(); j++) {
										String synName = syns.get(j).get(0);
										String synNameType = syns.get(j).get(1);
										Node synode = createNode();
										synode.setProperty("name", synName);
										synode.setProperty("uid", synode.getId());
										synode.setProperty("nametype", synNameType);
										synode.setProperty("source", sourcename);
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
				for (int i = 0; i < templines.size(); i++) {
					StringTokenizer st = new StringTokenizer(templines.get(i), "\t|\t");
					int numtok = st.countTokens();
					String inputId = st.nextToken();
					String nexttok = st.nextToken();
					String inputParentId = "";
					String inputName = "";
					String rank = "";
					Node tnode = createNode();
					//if it equals life it won't have a parent
					if (nexttok.equals("life") == false) {
						inputParentId = nexttok;
						inputName = st.nextToken();
						if (numtok == 4) {
							tnode.setProperty("rank", st.nextToken());
						}
						parents.put(inputId, inputParentId);
					} else {//root
						inputName = nexttok;
						System.out.println("created root node and metadata link");
						metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
					}
					tnode.setProperty("name", inputName);
					//TODO: add the ability to input these from a source if they have already been set
					tnode.setProperty("uid", tnode.getId());
					tnode.setProperty("sourceid", inputId);
					tnode.setProperty("sourcepid", inputParentId);
					tnode.setProperty("source", sourcename);
					taxaByName.add(tnode, "name", inputName);
					dbnodes.put(inputId, tnode);
					// synonym processing
					if (synFileExists) {
						if (synonymhash.get(inputId) != null) {
							ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
							for (int j = 0; j < syns.size(); j++) {
								String synName = syns.get(j).get(0);
								String synNameType = syns.get(j).get(1);
								Node synode = createNode();
								synode.setProperty("name", synName);
								synode.setProperty("uid", synode.getId());
								synode.setProperty("nametype", synNameType);
								synode.setProperty("source", sourcename);
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
						for (int i = 0; i < temppar.size(); i++) {
							try {
								Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
								rel.setProperty("source", sourcename);
								rel.setProperty("childid", temppar.get(i));
								rel.setProperty("parentid", parents.get(temppar.get(i)));
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
				for (int i = 0; i < temppar.size(); i++) {
					try {
						Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
						rel.setProperty("source", sourcename);
						rel.setProperty("childid", temppar.get(i));
						rel.setProperty("parentid", parents.get(temppar.get(i)));
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
			for (int i = 0; i < barrierNodes.size(); i++) {
				for (Node currentNode : CHILDREN_TRAVERSAL.traverse(barrierNodes.get(i)).nodes()) {
					currentNode.setProperty("taxcode", barrierNodesMap.get(barrierNodes.get(i).getProperty("name")));
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
		
		//start the mrcas
		System.out.println("calculating mrcas");
		try {
			tx = graphDb.beginTx();
			initMrcaForTipsAndPO(metadatanode.getSingleRelationship(RelType.METADATAFOR, Direction.OUTGOING).getEndNode());
			tx.success();
		} finally {
			tx.finish();
		}
	}

	/**
	 * Reads a taxonomy file with rows formatted as:
	 *	ott_id\t|\tott_parent_id\t|\tName\t|\trank with spaces allowed\t|\tsources\t|\tunique_name\n
	 *
	 *  OTT identifier - these have been kept stable relative to OTToL 1.0
	 *  OTT identifier for the parent of this taxon, or empty if none
	 *  Name (e.g. "Rana palustris")
	 *  Rank ("genus" etc.)
	 *  Sources - this takes the form tag:id,tag:id where tag is a short string identifying the source taxonomy (currently just "ncbi" or "gbif") and id is the numeric accession number within that taxonomy. Examples: ncbi:8404,gbif:2427185 ncbi:1235509
	 *  Unique name - if the name is a homonym, then the name qualified with its rank and the name of its parent taxon, e.g. "Roperia (genus in family Hemidiscaceae)"
	 *
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
	public void loadOTTIntoGraph(String sourcename, String filename, String synonymfile) {
		String str = "";
		int count = 0;
		Transaction tx;
		ArrayList<String> templines = new ArrayList<String>();
		HashMap<String, ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if (synonymfile.length() > 0) {
			synFileExists = true;
		}
		//preprocess the synonym file
		//key is the id from the taxonomy, the array has the synonym and the type of synonym
		if (synFileExists) {
			synonymhash = new HashMap<String, ArrayList<ArrayList<String>>>();
			try {
				BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
				while ((str = sbr.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(str, "\t|\t");
					String name = st.nextToken();
					String id = st.nextToken();
					ArrayList<String> tar = new ArrayList<String>();
					tar.add(name);tar.add("from OTT");
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
		
		Node metadatanode = null;
		try {
			tx = beginTx();
			//create the metadata node
			try {
				metadatanode = createNode();
				metadatanode.setProperty("source", sourcename);
				metadatanode.setProperty("author", "open tree of life project");
				metadatanode.setProperty("weburl", "https://github.com/OpenTreeOfLife/opentree/wiki/Open-Tree-Taxonomy");
				metadatanode.setProperty("uri", "");
				metadatanode.setProperty("urlprefix", '"');
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
						for (int i = 0; i < templines.size(); i++) {
							StringTokenizer st = new StringTokenizer(templines.get(i), "\t|\t");
							int numtok = st.countTokens();
							String inputId = st.nextToken();
							String nexttok = st.nextToken();
							String inputParentId = "";
							String rank = "";
							String inputName = "";
							String input_sources = "";
							String uniqname = "";
							Node tnode = createNode();
							//if it equals life it won't have a parent
							if (nexttok.equals("life") == false) {
								inputParentId = nexttok;
								inputName = st.nextToken();
								parents.put(inputId, inputParentId);
							} else {//root
								inputName = nexttok;
								System.out.println("created root node and metadata link");
								metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
							}
							tnode.setProperty("name", inputName);
							tnode.setProperty("uid", inputId);
							tnode.setProperty("sourceid", inputId);
							tnode.setProperty("sourcepid", inputParentId);
							tnode.setProperty("source", sourcename);
							rank = st.nextToken();
							input_sources = st.nextToken();
							tnode.setProperty("rank",rank);
							tnode.setProperty("input_sources",input_sources);
							if(st.hasMoreTokens()){
								uniqname = st.nextToken();
								tnode.setProperty("uniqname",uniqname);
							}
							taxaByName.add(tnode, "name", inputName);
							dbnodes.put(inputId, tnode);
							// synonym processing
							if (synFileExists) {
								if (synonymhash.get(inputId) != null) {
									ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
									for (int j = 0; j < syns.size(); j++) {
										String synName = syns.get(j).get(0);
										String synNameType = syns.get(j).get(1);
										Node synode = createNode();
										synode.setProperty("name", synName);
										synode.setProperty("uid", synode.getId());
										synode.setProperty("nametype", synNameType);
										synode.setProperty("source", sourcename);
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
				for (int i = 0; i < templines.size(); i++) {
					StringTokenizer st = new StringTokenizer(templines.get(i), "\t|\t");
					int numtok = st.countTokens();
					String inputId = st.nextToken();
					String nexttok = st.nextToken();
					String inputParentId = "";
					String rank = "";
					String inputName = "";
					String input_sources = "";
					String uniqname = "";
					Node tnode = createNode();
					//if it equals life it won't have a parent
					if (nexttok.equals("life") == false) {
						inputParentId = nexttok;
						inputName = st.nextToken();
						parents.put(inputId, inputParentId);
					} else {//root
						inputName = nexttok;
						System.out.println("created root node and metadata link");
						metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
					}
					tnode.setProperty("name", inputName);
					tnode.setProperty("uid", inputId);
					tnode.setProperty("sourceid", inputId);
					tnode.setProperty("sourcepid", inputParentId);
					tnode.setProperty("source", sourcename);
					rank = st.nextToken();
					input_sources = st.nextToken();
					tnode.setProperty("rank",rank);
					tnode.setProperty("input_sources",input_sources);
					if(st.hasMoreTokens()){
						uniqname = st.nextToken();
						tnode.setProperty("uniqname",uniqname);
					}
					taxaByName.add(tnode, "name", inputName);
					dbnodes.put(inputId, tnode);
					// synonym processing
					if (synFileExists) {
						if (synonymhash.get(inputId) != null) {
							ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
							for (int j = 0; j < syns.size(); j++) {
								String synName = syns.get(j).get(0);
								String synNameType = syns.get(j).get(1);
								Node synode = createNode();
								synode.setProperty("name", synName);
								synode.setProperty("uid", synode.getId());
								synode.setProperty("nametype", synNameType);
								synode.setProperty("source", sourcename);
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
						for (int i = 0; i < temppar.size(); i++) {
							try {
								Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
								rel.setProperty("source", sourcename);
								rel.setProperty("childid", temppar.get(i));
								rel.setProperty("parentid", parents.get(temppar.get(i)));
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
				for (int i = 0; i < temppar.size(); i++) {
					try {
						Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
						rel.setProperty("source", sourcename);
						rel.setProperty("childid", temppar.get(i));
						rel.setProperty("parentid", parents.get(temppar.get(i)));
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
			for (int i = 0; i < barrierNodes.size(); i++) {
				for (Node currentNode : CHILDREN_TRAVERSAL.traverse(barrierNodes.get(i)).nodes()) {
					currentNode.setProperty("taxcode", barrierNodesMap.get(barrierNodes.get(i).getProperty("name")));
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
		
		//start the mrcas
		System.out.println("calculating mrcas");
		try {
			tx = graphDb.beginTx();
			initMrcaForTipsAndPO(metadatanode.getSingleRelationship(RelType.METADATAFOR, Direction.OUTGOING).getEndNode());
			tx.success();
		} finally {
			tx.finish();
		}
	}

	
	
	HashMap<String, ArrayList<String>> globalchildren = null;
	HashMap<String,String> globalidnamemap = null;
	PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForTypes(RelType.TAXCHILDOF, Direction.OUTGOING), 10000);
	HashMap<Node,Node> lastexistingmatchparents = new HashMap<Node,Node>();
	

	
	/*
	 * for adding a taxonomy that won't connect to the life node
	 * goes to different index
	 * index gets stored as sourcename,name instead of name,name
	 */
	public void addDisconnectedTaxonomyToGraph(String sourcename, String filename, String synonymfile) {
		String str = "";
		int count = 0;
		Transaction tx = null;
		ArrayList<String> templines = new ArrayList<String>();
		HashMap<String,ArrayList<ArrayList<String>>> synonymhash = null;
		boolean synFileExists = false;
		if (synonymfile.length() > 0) {
			synFileExists = true;
		}
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
					tar.add(name);
					tar.add(type);
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

		// just setting source metadata manually for now
		String author = "";
		String weburl = "";
		String uri = "";
		String urlPrefix = "";
		if (sourcename == "ncbi") {
			author = "no one";
			weburl = "http://ncbi.nlm.nih.gov";
			uri = "";
			urlPrefix = "http://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=";
		} else if (sourcename == "gbif") {
			author = "gbif";
			weburl = "http://www.gbif.org/";
			uri = "";
			urlPrefix = "http://ecat-dev.gbif.org/usage/";		    
		}
		
		Index<Node> taxSources = ALLTAXA.getNodeIndex(NodeIndexDescription.TAX_SOURCES);
		//can plug into these by not saying name but the source itself
		Index<Node> taxaByName = ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
		Index<Node> taxaBySynonym = ALLTAXA.getNodeIndex(NodeIndexDescription.TAXON_BY_SYNONYM);
		Node metadatanode = null;
		try {
			tx = beginTx();
			//create the metadata node
			try {
				metadatanode = createNode();
				metadatanode.setProperty("source", sourcename);
				metadatanode.setProperty("author", author);
				metadatanode.setProperty("weburl", weburl);
				metadatanode.setProperty("uri", uri);
				metadatanode.setProperty("urlprefix", urlPrefix);
				//taxSourceIndex.add(metadatanode, "source", sourcename);
				System.out.println("source: " + sourcename);
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
						for (int i = 0; i < templines.size(); i++) {
							StringTokenizer st = new StringTokenizer(templines.get(i), "\t|\t");
							int numtok = st.countTokens();
							String inputId = st.nextToken();
							String nexttok = st.nextToken();
							String inputParentId = "";
							String inputName = "";
							String rank = "";
							Node tnode = createNode();
							//if it equals life it won't have a parent
							if (nexttok.equals("life") == false) {
								inputParentId = nexttok;
								inputName = st.nextToken();
								if (numtok == 4) {
									tnode.setProperty("rank", st.nextToken());
								}
								parents.put(inputId, inputParentId);
							} else {//root
								inputName = nexttok;
								System.out.println("created root node and metadata link");
								metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
							}
							tnode.setProperty("name", inputName);
							//TODO: add the ability to input these from a source if they have already been set
							tnode.setProperty("uid", tnode.getId());
							tnode.setProperty("sourceid", inputId);
							tnode.setProperty("sourcepid", inputParentId);
							tnode.setProperty("source", sourcename);
							taxaByName.add(tnode, sourcename, inputName);
							dbnodes.put(inputId, tnode);
							// synonym processing
							if (synFileExists) {
								if (synonymhash.get(inputId) != null) {
									ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
									for (int j = 0; j < syns.size(); j++) {
										String synName = syns.get(j).get(0);
										String synNameType = syns.get(j).get(1);
										Node synode = createNode();
										synode.setProperty("name", synName);
										synode.setProperty("uid", synode.getId());
										synode.setProperty("nametype", synNameType);
										synode.setProperty("source", sourcename);
										synode.createRelationshipTo(tnode, RelType.SYNONYMOF);
										taxaBySynonym.add(tnode, sourcename, synName);
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
				for (int i = 0; i<templines.size(); i++) {
					StringTokenizer st = new StringTokenizer(templines.get(i), "\t|\t");
					int numtok = st.countTokens();
					String inputId = st.nextToken();
					String nexttok = st.nextToken();
					String inputParentId = "";
					String inputName = "";
					String rank = "";
					Node tnode = createNode();
					//if it equals life it won't have a parent
					if (nexttok.equals("life") == false) {
						inputParentId = nexttok;
						inputName = st.nextToken();
						if (numtok == 4) {
							tnode.setProperty("rank", st.nextToken());
						}
						parents.put(inputId, inputParentId);
					} else {//root
						inputName = nexttok;
						System.out.println("created root node and metadata link");
						metadatanode.createRelationshipTo(tnode, RelType.METADATAFOR);
					}
					tnode.setProperty("name", inputName);
					//TODO: add the ability to input these from a source if they have already been set
					tnode.setProperty("uid", tnode.getId());
					tnode.setProperty("sourceid", inputId);
					tnode.setProperty("sourcepid", inputParentId);
					tnode.setProperty("source", sourcename);
					taxaByName.add(tnode, sourcename, inputName);
					dbnodes.put(inputId, tnode);
					// synonym processing
					if (synFileExists) {
						if (synonymhash.get(inputId) != null) {
							ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
							for (int j = 0; j < syns.size(); j++) {
								String synName = syns.get(j).get(0);
								String synNameType = syns.get(j).get(1);
								Node synode = createNode();
								synode.setProperty("name", synName);
								synode.setProperty("uid", synode.getId());
								synode.setProperty("nametype", synNameType);
								synode.setProperty("source", sourcename);
								synode.createRelationshipTo(tnode, RelType.SYNONYMOF);
								taxaBySynonym.add(tnode, sourcename, synName);
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
						for (int i = 0; i < temppar.size(); i++) {
							try {
								Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
								rel.setProperty("source", sourcename);
								rel.setProperty("childid", temppar.get(i));
								rel.setProperty("parentid", parents.get(temppar.get(i)));
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
				for (int i = 0; i < temppar.size(); i++) {
					try {
						Relationship rel = dbnodes.get(temppar.get(i)).createRelationshipTo(dbnodes.get(parents.get(temppar.get(i))), RelType.TAXCHILDOF);
						rel.setProperty("source", sourcename);
						rel.setProperty("childid", temppar.get(i));
						rel.setProperty("parentid", parents.get(temppar.get(i)));
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
		//no need for barrier things because these won't be searched
		//start the mrcas
		System.out.println("calculating mrcas");
		try {
			tx = graphDb.beginTx();
			initMrcaForTipsAndPO(metadatanode.getSingleRelationship(RelType.METADATAFOR, Direction.OUTGOING).getEndNode());
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	/**
	 * We are checking the validity of the taxonomy that is loaded.
	 * Validity is based on where there are nodes that have multiple parents
	 * within a source
	 * 
	 * @param sourcename this is the source that we are checking the validity of
	 */
	public void verifyLoadedTaxonomy(String sourcename) {
		//get life node
		//traverse starting at life, checking to see if any of the nodes have multiple parents if looking at the source from sourcename
		Node startnode = getLifeNode();
		TraversalDescription MRCACHILDOF_TRAVERSAL = Traversal.description()
		        .relationships( RelType.TAXCHILDOF,Direction.INCOMING );
		for (Node friendnode : MRCACHILDOF_TRAVERSAL.traverse(startnode).nodes()) {
			int count = 0;
			for (Relationship rel: friendnode.getRelationships(Direction.OUTGOING, RelType.TAXCHILDOF)) {
				if (((String)rel.getProperty("source")).compareTo(sourcename) == 0) {
					count += 1;
				}
				if (rel.getEndNode().getId() == rel.getStartNode().getId())
					System.out.println("invalid (self cycle): " + friendnode);
			}
			if (count > 1) {
				System.out.println("invalid (multiple parents): " + friendnode);
			}
		}
	}
	
	/**
	 * We are checking the validity of the taxonomy that is loaded.
	 * Validity is based on where there are nodes that have multiple parents
	 * within a source
	 * 
	 * @param sourcename this is the source that we are checking the validity of
	 */
	public void verifyMainTaxonomy() {
		//get life node
		//traverse starting at life, checking to see if any of the nodes have multiple parents if looking at the source from sourcename
		Node startnode = getLifeNode();
		TraversalDescription MRCACHILDOF_TRAVERSAL = Traversal.description()
		        .relationships( RelType.TAXCHILDOF,Direction.INCOMING );
		for (Node friendnode : MRCACHILDOF_TRAVERSAL.traverse(startnode).nodes()) {
			int count = 0;
			for (Relationship rel: friendnode.getRelationships(Direction.OUTGOING, RelType.TAXCHILDOF)) {
				count += 1;
				if (rel.getEndNode().getId() == rel.getStartNode().getId()) {
					System.out.println("invalid (self cycle): " + friendnode);
				}
			}
			if (count > 1) {
				System.out.println("invalid (multiple parents): " + friendnode);
			}
		}
	}
	
	/**
	 * delete all the mrcas and nested mrcas
	 * @param dbnode
	 */
	public void removeMRCAs(Node dbnode) {
		TraversalDescription TAXCHILDOF_TRAVERSAL = Traversal.description()
		        .relationships( RelType.TAXCHILDOF,Direction.INCOMING );
		Transaction tx = graphDb.beginTx();
		int count = 0;
		try {
			for (Node frnode: TAXCHILDOF_TRAVERSAL.traverse(dbnode).nodes()) {
				frnode.removeProperty("mrca");
				frnode.removeProperty("nested_mrca");
				if (count % transaction_iter == 0) {
					System.out.println(count);
					tx.success();
					tx.finish();
					tx = graphDb.beginTx();
				}
				count += 1;
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	private Transaction	general_tx;
	private int cur_tran_iter = 0;
	public void initMrcaForTipsAndPO(Node startnode) {
		general_tx = graphDb.beginTx();
		//start from the node called root
//		Node startnode = getLifeNode();
		try {
			//root should be the taxonomy startnode
			TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
			        .relationships( RelType.TAXCHILDOF,Direction.INCOMING );
			for (Node friendnode : CHILDOF_TRAVERSAL.traverse(startnode).nodes()) {
				Node taxparent = getAdjNodeFromFirstRelationship(friendnode, RelType.TAXCHILDOF, Direction.OUTGOING);
				if (taxparent != null) {
					Node firstchild = getAdjNodeFromFirstRelationship(friendnode, RelType.TAXCHILDOF, Direction.INCOMING);
					if (firstchild == null) {//leaf
						long [] tmrcas = {friendnode.getId()};
						friendnode.setProperty("mrca", tmrcas);
						long [] ntmrcas = {};
						friendnode.setProperty("nested_mrca", ntmrcas);
					}
					cur_tran_iter += 1;
					if (cur_tran_iter % transaction_iter == 0) {
						general_tx.success();
						general_tx.finish();
						general_tx = graphDb.beginTx();
						System.out.println("cur transaction: " + cur_tran_iter);
					}
				} else {
					System.out.println(friendnode+"\t" + friendnode.getProperty("name"));
				}
			}
			general_tx.success();
		} finally {
			general_tx.finish();
		}
		//start the mrcas
		System.out.println("calculating mrcas");
		try {
			general_tx = graphDb.beginTx();
			postorderAddMRCAsTax(startnode);
			general_tx.success();
		} finally {
			general_tx.finish();
		}
	}
	
	static public Node getAdjNodeFromFirstRelationship(Node nd, RelationshipType relType, Direction dir) {
		for (Relationship rel: nd.getRelationships(relType, dir)) {
			if (dir == Direction.OUTGOING) {
				return rel.getEndNode();
			} else {
				return rel.getStartNode();
			}
		}
		return null;
	}
	
	static public Node getAdjNodeFromFirstRelationshipBySource(Node nd, RelationshipType relType, Direction dir,  String src) {
		for (Relationship rel : nd.getRelationships(relType, dir)) {
			if (((String)rel.getProperty("source")).equals(src)) {
				if (dir == Direction.OUTGOING) {
					return rel.getEndNode();
				} else {
					return rel.getStartNode();
				}
			}
		}
		return null;
	}
	
	/**
	 * for initial taxonomy to tree processing.  adds a mrca->long[]  property
	 *	to the node and its children (where the elements of the array are the ids
	 *	of graph of life nodes). The property is is the union of the mrca properties
	 *	for the subtree. So the leaves of the tree must already have their mrca property
	 *	filled in!
	 *
	 * @param dbnode should be a node in the graph-of-life (has incoming MRCACHILDOF relationship)
	 */
	private void postorderAddMRCAsTax(Node dbnode) {
		//traversal incoming and record all the names
		for (Relationship rel : dbnode.getRelationships(Direction.INCOMING,RelType.TAXCHILDOF)) {
			Node tnode = rel.getStartNode();
			postorderAddMRCAsTax(tnode);
		}
		//could make this a hashset if dups become a problem
		ArrayList<Long> mrcas = new ArrayList<Long> ();
		ArrayList<Long> nested_mrcas = new ArrayList<Long>();
		if (dbnode.hasProperty("mrca") == false) {
			for (Relationship rel: dbnode.getRelationships(Direction.INCOMING,RelType.TAXCHILDOF)) {
				Node tnode = rel.getStartNode();
				long[] tmrcas = (long[])tnode.getProperty("mrca");
				for (int j = 0; j < tmrcas.length; j++) {
					mrcas.add(tmrcas[j]);
				}
				long[] nestedtmrcas = (long[])tnode.getProperty("nested_mrca");
				for (int j = 0; j < nestedtmrcas.length; j++) {
					nested_mrcas.add(nestedtmrcas[j]);
				}
			}
			long[] ret = new long[mrcas.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = mrcas.get(i).longValue();
			}
			try {
				dbnode.setProperty("mrca", ret);
			} catch(Exception e) {
				System.out.println(dbnode);
				System.out.println(ret.length);
				System.out.println(mrcas.size());
				for (Relationship rel: dbnode.getRelationships(Direction.INCOMING,RelType.TAXCHILDOF)) {
					Node tnode = rel.getStartNode();
					long[] tmrcas = (long[])tnode.getProperty("mrca");
					System.out.println("tmrcas: " + tmrcas.length + " " + rel);
					long[] nestedtmrcas = (long[])tnode.getProperty("nested_mrca");
					System.out.println("tmrcas: " + nestedtmrcas.length + " " + rel);
				}
				System.exit(0);
			}
			
			nested_mrcas.add(dbnode.getId());
			long[] ret2 = new long[nested_mrcas.size()];
			for (int i = 0; i < ret2.length; i++) {
				ret2[i] = nested_mrcas.get(i).longValue();
			}
			dbnode.setProperty("nested_mrca", ret2);
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

