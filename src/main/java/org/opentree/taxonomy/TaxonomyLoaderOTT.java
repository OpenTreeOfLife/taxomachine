package org.opentree.taxonomy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.OTTFlag;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.contexts.Nomenclature;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;

import java.util.Map;

/**
 * This is just a class for loading in the OTT taxonomy, as generated by smasher. This class extends the
 * TaxonomyLoaderBase, which has more general loading/validating functions. Extensions to facilitate loading
 * other taxonomies, if they become necessary, should be implemented in other separate classes.
 */
public class TaxonomyLoaderOTT extends TaxonomyLoaderBase {
	
	static String graphname;
	
	// ===== indexes. see setupIndexes method for more info
	
	// just record the original sources
	Index<Node> taxSources;
	
	// all taxa names
	Index<Node> taxaByName;
	Index<Node> taxaBySynonym;
	Index<Node> taxaByNameOrSynonym;
	Index<Node> taxaByNameHigher;
	Index<Node> taxaByNameOrSynonymHigher;

	// all taxa by other info
	Index<Node> taxaByOTTId;
	Index<Node> taxaByFlag;

	// all taxa rank-based
	Index<Node> taxaByRank;
	Index<Node> taxaByNameGenera;
	Index<Node> taxaByNameSpecies;

	// preferred names
	Index<Node> prefTaxaByName;
	Index<Node> prefTaxaBySynonym;
	Index<Node> prefTaxaByNameOrSynonym;	
	Index<Node> prefTaxaByNameHigher;
	Index<Node> prefTaxaByNameOrSynonymHigher;

	// preferred rank-based
	Index<Node> prefTaxaByRank;	
	Index<Node> prefTaxaByNameGenera;
	Index<Node> prefTaxaByNameSpecies;
	
	// deprecated taxa
	Index<Node> deprecatedTaxa = graphDb.getNodeIndex(TaxonomyNodeIndex.DEPRECATED_TAXA.indexName());
	
	HashMap<String, Node> dbnodes = new HashMap<String, Node>();
	HashMap<String, String> parents = new HashMap<String, String>();
	HashMap<String, OTTFlag> ottFlags;
	
	HashSet<Node> dubiousNodes = new HashSet<Node>();

	Node metadatanode = null;
	
	Transaction tx;
	
	String sourceName = "";
	
	HashMap<String, ArrayList<ArrayList<String>>> synonymhash = null;
	boolean synFileExists = false;
	
	// ===== install options
	
	private boolean addSynonyms = true;
	private boolean addBarrierNodes = true;
	private boolean buildPreferredIndexes = true; // requires that preferred rels are also built.
	private boolean buildPreferredRels = true;
	private boolean createOTTIdIndexes = true;
	
	// ========================================
	
	// basic traversal method
	final TraversalDescription CHILDOF_TRAVERSAL = Traversal.description()
			.relationships(TaxonomyRelType.TAXCHILDOF,Direction.OUTGOING);
	
	public TaxonomyLoaderOTT(GraphDatabaseAgent gdb) {
		super(gdb);
		buildFlagMap();
	}
	
	public TaxonomyLoaderOTT(GraphDatabaseService gds) {
		super(gds);
		buildFlagMap();
	}
	
	public void setCreateOTTIdIndexes (boolean createOTTIdIndexes) {
		this.createOTTIdIndexes = createOTTIdIndexes;
	}
	
	public void setAddSynonyms(boolean addSynonyms) {
		this.addSynonyms = addSynonyms;
	}

	public void setAddBarrierNodes (boolean addBarrierNodes) {
		this.addBarrierNodes = addBarrierNodes;
	}

	public void setbuildPreferredIndexes (boolean buildPreferredIndexes) {
		this.setbuildPreferredRels(true);
		this.buildPreferredIndexes = buildPreferredIndexes;
	}

	public void setbuildPreferredRels (boolean buildPreferredRels) {
		this.buildPreferredRels = buildPreferredRels;
	}

	public void loadDeprecatedTaxa(String deprecatedFile) {
		
		Transaction tx = graphDb.beginTx();
		
		Node deprecatedContainerNode = graphDb.createNode();
		deprecatedContainerNode.setProperty("name", "deprecated taxa");
		
		Node lifeNode = getLifeNode();
		if (lifeNode != null) {
			deprecatedContainerNode.createRelationshipTo(lifeNode, TaxonomyRelType.CONTAINEDBY);
		}

		int n = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(deprecatedFile));
			String curStr;
			while ((curStr = reader.readLine()) != null) {
				StringTokenizer tokenizer = new StringTokenizer(curStr, "\t");
				Long id = Long.valueOf(tokenizer.nextToken());
				String name = tokenizer.nextToken();
				String reason = tokenizer.nextToken();
				String sourceInfo = tokenizer.nextToken();
				
				Node d = graphDb.createNode();
				d.createRelationshipTo(deprecatedContainerNode, TaxonomyRelType.CONTAINEDBY);
				d.setProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), name);
				d.setProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName(), id);
				d.setProperty(TaxonomyProperty.REASON.propertyName(), reason);
				d.setProperty(TaxonomyProperty.SOURCE_INFO.propertyName(), sourceInfo);
				
				deprecatedTaxa.add(deprecatedContainerNode, TaxonomyProperty.NAME.propertyName(), name);
				deprecatedTaxa.add(deprecatedContainerNode, OTVocabularyPredicate.OT_OTT_ID.propertyName(), id);

				n++;
			}
			reader.close();
			tx.success();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		tx.finish();
		System.out.println("added " + n + " deprecated ids to the database.");
	}
	
	/**
	 * Just a wrapper for the no synonyms case. See javadoc for this method with the synonyms option for full description.
	 * @param sourcename
	 * @param filename
	 */
	public void loadOTTIntoGraph(String sourcename, String filename) {
		setAddSynonyms(false);
		loadOTTIntoGraph(sourcename, filename, "");
	}
	
	/**
	 * Reads a taxonomy file with rows formatted as:
	 *	ott_id\tott_parent_id\tName\trank with spaces allowed\tsources\tunique_name\n TODO: add flags to this description
	 *
	 *  OTT identifier - these have been kept stable relative to OTT 1.0
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
		
		setupIndexes(); // have to call this here instead of on construction so that we don't make indexes we're not going to use
		
		this.sourceName = sourcename;
		String str = "";
		int count = 0;
		ArrayList<String> templines = new ArrayList<String>();

		if (addSynonyms) {
			if (synonymfile.length() > 0) {
				synFileExists = true;
			}

			// preprocess the synonym file
			// key is the id from the taxonomy, the array has the synonym and the type of synonym
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
		}
		
		if (buildPreferredIndexes && !buildPreferredRels) {
			throw new UnsupportedOperationException("Cannot build the preferred indexes without building the preferred relationships.");
		}
		
		//create the metadata node
		tx = beginTx();
		try {
			createMetadataNode();
			tx.success();
		} finally {
			tx.finish();
		}

		// process the incoming lines in batches
		try {
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
							processOTTInputLine(templines.get(i));
						}
						tx.success();
					} finally {
						tx.finish();
					}
					templines.clear();
				}
			}
			br.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		// process any remaining lines from the last batch
		tx = beginTx();
		try {
			for (int i = 0; i < templines.size(); i++) {
				processOTTInputLine(templines.get(i));
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

						processOTTRels(temppar.get(i));
						
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

				processOTTRels(temppar.get(i));
				
			}
			tx.success();
		} finally {
			tx.finish();
		}

		
		if (addBarrierNodes) {
			// mark all of the barrier nodes with additional metadata
			System.out.println("setting barrier nodes");
			BarrierNodes bn = new BarrierNodes(this);
			bn.initializeBarrierNodes();
			Map<Node, Nomenclature> bnMap = bn.getBarrierNodeToNomenclatureMap();
			TraversalDescription CHILDREN_TRAVERSAL = Traversal.description()
					.relationships( TaxonomyRelType.TAXCHILDOF,Direction.INCOMING );
			tx = beginTx();
			try {
				for (Node bNode : bnMap.keySet()) {
					System.out.println("setting tax code for barrier node " + bNode + " to " + bnMap.get(bNode).code);
					for (Node currentNode : CHILDREN_TRAVERSAL.traverse(bNode).nodes()) {
						currentNode.setProperty("taxcode", bnMap.get(bNode).code);
					}
				}
				tx.success();
			} finally {
				tx.finish();
			}
		}
		
		// start the mrcas
		System.out.println("calculating mrcas");
		try {
			tx = graphDb.beginTx();
			initMrcaForTipsAndPO(metadatanode.getSingleRelationship(TaxonomyRelType.METADATAFOR, Direction.OUTGOING).getEndNode());
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	/**
	 * add relationships for a processed taxon
	 * @param childId
	 */
	private void processOTTRels(String childId) {
		
		Node parentNode = dbnodes.get(parents.get(childId));
		if (parentNode == null) {

			Node graphRootNode = dbnodes.get(childId);
			
			// special case for the taxonomy root - the 'life' node in non-subsetted taxonomies, in other cases name could be anything
			metadatanode.createRelationshipTo(graphRootNode, TaxonomyRelType.METADATAFOR);
			System.out.println("linked metadata node to " + graphRootNode);
			
		} else {
			Relationship rel = dbnodes.get(childId).createRelationshipTo(parentNode, TaxonomyRelType.TAXCHILDOF);
			rel.setProperty("source", sourceName);
			rel.setProperty("childid", childId);
			rel.setProperty("parentid", parents.get(childId));

			if (buildPreferredRels) {
				// don't need to wait to makeottol anymore
				if (!dubiousNodes.contains(dbnodes.get(childId))) {
					Relationship prefRel = dbnodes.get(childId).createRelationshipTo(dbnodes.get(parents.get(childId)), TaxonomyRelType.PREFTAXCHILDOF);
					prefRel.setProperty("source", sourceName);
					prefRel.setProperty("childid", childId);
					prefRel.setProperty("parentid", parents.get(childId));
				}
			}
		}
	}
	
	/**
	 * process an input line from the ott taxonomy file
	 * @param line
	 */
	private void processOTTInputLine(String line) {
		
		// split the input line all the way to the end. we expect trailing empty strings when flags and uniqname are not set
		String[] tokens = line.split("\t\\|\t",-1);
		int i = 0;
		String inputId = tokens[i++];
		
		// skip the header line if it exists
		if (inputId.equals("uid")) {
			return;
		}
		
		String inputParentId = tokens[i++];
		String inputName = tokens[i++];
		String rank = tokens[i++];
		String inputSources = tokens[i++];
		String uniqname = tokens[i++];
		
		// the last token is the flags. don't try parsing them unless they exist
		String[] flags = tokens[i] != null ? tokens[i].split("\\,") : null;
		
		// we determine these bits later based on flags
		boolean dubious = false;
		boolean forceVisible = false;
		
		Node tnode = createNode();
		parents.put(inputId, inputParentId);

		// TODO: fix these to use OTPropertyPredicate model
		tnode.setProperty("name", inputName);
		tnode.setProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName(), Long.valueOf(inputId));
		tnode.setProperty("sourceid", inputId);
		tnode.setProperty("sourcepid", inputParentId);
		tnode.setProperty("source", sourceName);
		tnode.setProperty("rank", rank);
		tnode.setProperty("input_sources", inputSources);
		tnode.setProperty("uniqname", uniqname);

		if (flags.length > 0) {
			for (String label : flags) {
				
				if (!label.equals("")) {

					// record each flag as a node property, whether we recognize it or not
					tnode.setProperty(label, true);
					taxaByFlag.add(tnode, "flag", label);
	
					// if we recognize the flag, check if it implies additional action
					if (ottFlags.containsKey(label)) {
						OTTFlag flag = ottFlags.get(label);
	
						// if forced visible, override any previous dubious setting
						if (flag == OTTFlag.FORCE_VISIBLE) {
							forceVisible = true;
							if (dubiousNodes.contains(tnode)) {
								dubious = false;
								dubiousNodes.remove(tnode);
							}
						}
					
						// if this flag indicates suppression and taxon has not already been flagged force visible, mark as dubious
						if (!flag.includeInPrefIndexes && !forceVisible) {
							dubious = true;
							dubiousNodes.add(tnode);
						}
					} else {
						System.out.println("WARNING: found unrecognized flag: " + label);
					}
				}
			}
		}
				
		tnode.setProperty("dubious", dubious);
		if (dubious) {
			taxaByFlag.add(tnode, "flag", "dubious");
		}
		
		taxaByName.add(tnode, "name", inputName);
		taxaByRank.add(tnode, "rank", rank);
		
		if (createOTTIdIndexes) {
			taxaByOTTId.add(tnode, OTVocabularyPredicate.OT_OTT_ID.propertyName(), Long.valueOf(inputId));
		}
		
		if (addSynonyms) {
			taxaByNameOrSynonym.add(tnode, "name", inputName);
		}
		
		// add to the rank-specific indexes
        if (isSpecific(rank)) {
        	taxaByNameSpecies.add(tnode, "name", inputName);

        } else if (rank.equals("genus")) {
        	taxaByNameGenera.add(tnode, "name", inputName);
        	taxaByNameHigher.add(tnode, "name", inputName);
        	if (addSynonyms) {
        		taxaByNameOrSynonymHigher.add(tnode, "name", inputName);
        	}

        } else {
        	taxaByNameHigher.add(tnode, "name", inputName);
        	if (addSynonyms) {
        		taxaByNameOrSynonymHigher.add(tnode, "name", inputName);
        	}
        }
		
        // create preferred indexes... this is a bit redundant but allows faster searches
        // when the goal is only to search non-hidden taxa (smaller indexes == faster searches)
		if (buildPreferredIndexes && !dubious) {
			prefTaxaByName.add(tnode, "name", inputName);
			prefTaxaByRank.add(tnode, "rank", rank);

			if (addSynonyms) {
				prefTaxaByNameOrSynonym.add(tnode, "name", inputName);
			}
			
			// add to the rank-specific indexes
	        if (isSpecific(rank)) {
	        	prefTaxaByNameSpecies.add(tnode, "name", inputName);

	        } else if (rank.equals("genus")) {
	        	prefTaxaByNameGenera.add(tnode, "name", inputName);
	        	prefTaxaByNameHigher.add(tnode, "name", inputName);
	        	if (addSynonyms) {
	        		prefTaxaByNameOrSynonymHigher.add(tnode, "name", inputName);
	        	}

	        } else {
	        	prefTaxaByNameHigher.add(tnode, "name", inputName);
	        	if (addSynonyms) {
	        		prefTaxaByNameOrSynonymHigher.add(tnode, "name", inputName);
	        	}
	        }
		}
		
		dbnodes.put(inputId, tnode);
		
		if (addSynonyms) {
			// synonym processing
			if (synFileExists) {
				if (synonymhash.get(inputId) != null) {
					ArrayList<ArrayList<String>> syns = synonymhash.get(inputId);
					for (int j = 0; j < syns.size(); j++) {
						String synName = syns.get(j).get(0);
						String synNameType = syns.get(j).get(1);
						Node synode = createNode();
						synode.setProperty("name", synName);
						
						synode.setProperty("nametype", synNameType);
						synode.setProperty("source", sourceName);
						synode.createRelationshipTo(tnode, TaxonomyRelType.SYNONYMOF);
						
						// indexing
						taxaBySynonym.add(tnode, "name", synName);
						taxaByNameOrSynonym.add(tnode, "name", synName);
			            if (!isSpecific(rank)) {
			            	taxaByNameOrSynonymHigher.add(tnode, "name", synName);
			            }
	
			            // additional indexing (smaller indexes) for non-hidden taxa
						if (buildPreferredIndexes && !dubious) {
							prefTaxaBySynonym.add(tnode,"name",synName);
							prefTaxaByNameOrSynonym.add(tnode,"name",synName);
				            if (!isSpecific(rank)) {
				            	prefTaxaByNameOrSynonymHigher.add(tnode, "name", synName);
				            }
						}
					}
				}
			}
		}
	}

	/**
	 * Make a map of flags by their tag strings coming in from the file so we can quickly look them up during import
	 */
	private void buildFlagMap() {
		ottFlags = new HashMap<String, OTTFlag>();
		for (OTTFlag f : OTTFlag.values()) {
			ottFlags.put(f.label, f);
		}
		System.out.println("\nUsing flags:");
		for (String flag : ottFlags.keySet()) {
			System.out.println(flag);
		}
		System.out.println("");
	}
	
	private void createMetadataNode() {
		metadatanode = createNode();
		metadatanode.setProperty("source", sourceName);
		metadatanode.setProperty("author", "open tree of life project");
		metadatanode.setProperty("weburl", "https://github.com/OpenTreeOfLife/opentree/wiki/Open-Tree-Taxonomy");
		metadatanode.setProperty("uri", "");
		metadatanode.setProperty("urlprefix", '"');
		taxSources.add(metadatanode, "source", sourceName);
	}
	
	/**
	 * Set up indexes according to the loading options set
	 */
	private void setupIndexes() {
		
		// always set up
		taxSources = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXONOMY_SOURCES);
		taxaByName = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME);
		taxaByRank = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_RANK);
		taxaByFlag = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_FLAG);

		taxaByNameSpecies = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_SPECIES);
		taxaByNameGenera = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_GENERA);
		taxaByNameHigher = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_HIGHER);

		// only for synonyms
		if (addSynonyms) {
			taxaBySynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_SYNONYM);
			taxaByNameOrSynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_OR_SYNONYM);
			taxaByNameOrSynonymHigher = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_OR_SYNONYM_HIGHER);
		}

		// only for ott id option
		if (createOTTIdIndexes) {
			taxaByOTTId = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_OTT_ID);
		}

		// only for preferred option
		if (buildPreferredIndexes) {
			prefTaxaByName = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME);
			prefTaxaByRank = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_RANK);
			prefTaxaByNameSpecies = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_SPECIES);
			prefTaxaByNameGenera = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_GENERA);
			prefTaxaByNameHigher = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_HIGHER);

			// only if also doing synonyms
			if (addSynonyms) {
				prefTaxaBySynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_SYNONYM);
				prefTaxaByNameOrSynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
				prefTaxaByNameOrSynonymHigher = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM_HIGHER);
			}
		}
	}
}

