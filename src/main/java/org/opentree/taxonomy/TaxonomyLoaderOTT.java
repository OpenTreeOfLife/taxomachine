package org.opentree.taxonomy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.opentree.graphdb.GraphDatabaseAgent;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.constants.TaxonomyProperty;
import org.opentree.taxonomy.constants.TaxonomyRelType;
import org.opentree.taxonomy.contexts.Nomenclature;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;

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
	Index<Node> synonymNodesBySynonym;
	Index<Node> synonymNodesBySynonymHigher;

	// all taxa by other info
	Index<Node> taxaByOTTId;
	Index<Node> taxaBySourceId;
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
	Index<Node> prefSynonymNodesBySynonym;
	Index<Node> prefSynonymNodesBySynonymHigher;

	// preferred rank-based
	Index<Node> prefTaxaByRank;	
	Index<Node> prefTaxaByNameGenera;
	Index<Node> prefTaxaByNameSpecies;
	
	// deprecated taxa
	Index<Node> deprecatedTaxa;
	
//	HashMap<String, Node> dbnodes = new HashMap<String, Node>();
//	HashMap<String, String> parents = new HashMap<String, String>();
	HashMap<Long, Node> dbNodeForOTTIdMap = new HashMap<Long, Node>();
	HashMap<Long, Long> parentOTTIdByChildOTTIdMap = new HashMap<Long, Long>();
	HashMap<String, OTTFlag> ottFlags;
	
	HashSet<Node> dubiousNodes = new HashSet<Node>();

	Node metadatanode = null;
	
	Transaction tx;
	
	String sourceName = "ott";

	
	HashMap<String, ArrayList<ArrayList<String>>> ottIdToSynonymNameMap = null;
	HashMap<Long, ArrayList<String>> ottIdToSynonymNamesMap = new HashMap<Long, ArrayList<String>>();
	boolean synFileExists = false;
	
	// ===== install options
	
	private boolean addSynonyms = true;
	private boolean addBarrierNodes = true;
	private boolean buildPreferredIndexes = true; // requires that preferred rels are also built.
	private boolean buildPreferredRels = true;
	private boolean createOTTIdIndexes = true;
	private boolean createSourceIdIndexes = true;
	
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
		
		setupIndexes();
		
		Transaction tx = graphDb.beginTx();
		
		Node deprecatedContainerNode = graphDb.createNode();
		deprecatedContainerNode.setProperty("description", "container node to which deprecated taxon nodes are attached");
		
		Node lifeNode = getTaxonomyRootNode();
		if (lifeNode != null) {
			deprecatedContainerNode.createRelationshipTo(lifeNode, TaxonomyRelType.CONTAINEDBY);
		}

		int n = 0;
		boolean observedHeaderLine = false;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(deprecatedFile));
			String curStr;
			while ((curStr = reader.readLine()) != null) {
				
				StringTokenizer tokenizer = new StringTokenizer(curStr, "\t");

				// skip the header line if we see it
				if (!observedHeaderLine) {
					String firstToken = tokenizer.nextToken();
					if (firstToken.equals("id")) {
						observedHeaderLine = true;
						continue;
					}
				}
				
				Long id = Long.valueOf(tokenizer.nextToken());
				String name = tokenizer.nextToken();
				String sourceInfo = tokenizer.nextToken();
				String reason = tokenizer.nextToken();
				
				Node d = graphDb.createNode();
				d.createRelationshipTo(deprecatedContainerNode, TaxonomyRelType.CONTAINEDBY);
				d.setProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), name);
				d.setProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName(), id);
				d.setProperty(TaxonomyProperty.REASON.propertyName(), reason);
				//use SOURCE_INFO rather than INPUT_SOURCES here (because node is deprecated?)
				d.setProperty(TaxonomyProperty.SOURCE_INFO.propertyName(), sourceInfo);
				d.setProperty(TaxonomyProperty.DEPRECATED.propertyName(), true);
				
				deprecatedTaxa.add(d, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), name);
				deprecatedTaxa.add(d, OTVocabularyPredicate.OT_OTT_ID.propertyName(), id);

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
	 *  Unique name - if the name is a homonym, then the name qualified with its rank and the name of an ancestral taxon it does not share with the other taxa that share its name, e.g. "Roperia (genus in family Hemidiscaceae) vs. Roperia (genus in subclass Apiidae)"
	 *
	 * Creates nodes and TAXCHILDOF relationships for each line.
	 * Nodes get a OTVocabularyPredicate.OT_OTT_TAXON_NAME property. Relationships get "source", "childid", "parentid" properties.
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
        System.out.format("sourcename = %s\n", sourcename);
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
				System.out.println("getting synonyms");
				ottIdToSynonymNamesMap = new HashMap<Long, ArrayList<String>>();
				try {
					BufferedReader sbr = new BufferedReader(new FileReader(synonymfile));
					while ((str = sbr.readLine()) != null) {
						
						StringTokenizer st = new StringTokenizer(str, "\t|\t");
						String name = st.nextToken();
						String idStr = st.nextToken();

						Long id = null;
						if (idStr != null && idStr.length() > 0 && !idStr.equals("uid")) {
							id = Long.valueOf(idStr);
						}

						if (id == null && !idStr.equals("uid")) {
							System.out.println("During synonym import, could not interpret id: " + idStr + " for synonym name " + name);
							continue;
						}
						
						if (ottIdToSynonymNamesMap.get(id) == null) {
							ottIdToSynonymNamesMap.put(id, new ArrayList<String>());
						}

						ottIdToSynonymNamesMap.get(id).add(name);
					}
					sbr.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
				System.out.println("found " + ottIdToSynonymNamesMap.size() + " synonyms");
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

		System.out.println("\nProcessing input files and adding nodes");
		
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

		System.out.println("\nAdding relationships");

		// add the relationships
		LinkedList<Long> temppar = new LinkedList<Long>();
		count = 0;
		for (Long key: dbNodeForOTTIdMap.keySet()) {
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
		
		// process any remaining lines for relationships
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
			System.out.println("\nSetting barrier nodes");
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
		System.out.println("\nCalculating mrca sets");
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
	private void processOTTRels(Long childId) {
		
		Node parentNode = dbNodeForOTTIdMap.get(parentOTTIdByChildOTTIdMap.get(childId));
		if (parentNode == null) {

			Node graphRootNode = dbNodeForOTTIdMap.get(childId);
			setTaxonomyRootNode(graphRootNode);
			
			// special case for the taxonomy root - the 'life' node in non-subsetted taxonomies, in other cases name could be anything
			metadatanode.createRelationshipTo(graphRootNode, TaxonomyRelType.METADATAFOR);
			System.out.println("linked metadata node to " + graphRootNode);
			
		} else {
			Relationship rel = dbNodeForOTTIdMap.get(childId).createRelationshipTo(parentNode, TaxonomyRelType.TAXCHILDOF);
			rel.setProperty("source", sourceName);
			rel.setProperty("childid", childId);
			rel.setProperty("parentid", parentOTTIdByChildOTTIdMap.get(childId));

			if (buildPreferredRels) {
				// don't need to wait to makeottol anymore
				if (!dubiousNodes.contains(dbNodeForOTTIdMap.get(childId))) {
					Relationship prefRel = dbNodeForOTTIdMap.get(childId).createRelationshipTo(dbNodeForOTTIdMap.get(parentOTTIdByChildOTTIdMap.get(childId)), TaxonomyRelType.PREFTAXCHILDOF);
					prefRel.setProperty("source", sourceName);
					prefRel.setProperty("childid", childId);
					prefRel.setProperty("parentid", parentOTTIdByChildOTTIdMap.get(childId));
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
		if (tokens.length == 1) {
			tokens = line.split("\t",-1);
		}
		if (tokens.length == 1) {
			throw new UnsupportedOperationException("The input file does not seem to be delimited in a known format (either \\t or \\t|\\t).");
		}
		int i = 0;
		String inputIdStr = tokens[i++];
		
		// skip the header line if it exists
		if (inputIdStr.equals("uid")) {
			return;
		}
		
//		System.out.println(line);
		
		Long inputId = Long.valueOf(inputIdStr);

		String inputParentIdStr = tokens[i++];
		Long inputParentId = inputParentIdStr.trim().equals("") ? null : Long.valueOf(inputParentIdStr);
		String inputName = tokens[i++];
		String rank = tokens[i++];
		String inputSources = tokens[i++];
		String uniqname = tokens[i++];
		
		// the last token is the flags. don't try parsing them unless they exist
		String[] flags = tokens[i] != null ? tokens[i].split("\\,") : null;
		
        processOTTData(inputId, inputParentId, inputName, rank, inputSources, uniqname, flags);
    }

    public void processOTTData(Long inputId, Long inputParentId, String inputName,
                               String rank, String inputSources, String uniqname, String[] flags) {

		// we determine these bits later based on flags
		boolean dubious = false;
		boolean forceVisible = false;
		
		Node tnode = createNode();
		parentOTTIdByChildOTTIdMap.put(inputId, inputParentId);

		tnode.setProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
		tnode.setProperty(OTVocabularyPredicate.OT_OTT_ID.propertyName(), inputId);
		tnode.setProperty(TaxonomyProperty.RANK.propertyName(), rank);
		tnode.setProperty(TaxonomyProperty.INPUT_SOURCES.propertyName(), inputSources);
		tnode.setProperty(TaxonomyProperty.UNIQUE_NAME.propertyName(), uniqname);

		// irrelevant for ott, individual sources are dealt with by smasher
//		tnode.setProperty("sourceid", inputId);
//		tnode.setProperty("sourcepid", inputParentId);
//		tnode.setProperty("source", sourceName);

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
				
		tnode.setProperty(TaxonomyProperty.DUBIOUS.propertyName(), dubious);
		if (dubious) {
			taxaByFlag.add(tnode, "flag", TaxonomyProperty.DUBIOUS.propertyName());
		}
		
		taxaByName.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
		taxaByRank.add(tnode, TaxonomyProperty.RANK.propertyName(), rank);
		
		if (createOTTIdIndexes) {
			taxaByOTTId.add(tnode, OTVocabularyPredicate.OT_OTT_ID.propertyName(), Long.valueOf(inputId));
		}
		
		if (createSourceIdIndexes) {
            for (String source : inputSources.split(","))
                if (source.contains(":") &&
                    !source.endsWith(":") &&
                    !source.startsWith("http"))
                    taxaBySourceId.add(tnode, TaxonomyProperty.SOURCE_ID.propertyName(), source);
		}
		
		if (addSynonyms) {
			taxaByNameOrSynonym.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
//			taxaByNameOrSynonym.add(tnode, TaxonomyProperty.NAME.propertyName(), inputName);
		}
		
		// add to the rank-specific indexes
        if (isSpecific(rank)) {
        	taxaByNameSpecies.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);

        } else if (rank.equals("genus")) {
        	taxaByNameGenera.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
        	taxaByNameHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
        	if (addSynonyms) {
    			// TODO: figure out what to do about indexing synonyms.. should we use "name" or "ot:ottTaxonName" as the property?
        		taxaByNameOrSynonymHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
//        		taxaByNameOrSynonymHigher.add(tnode, TaxonomyProperty.NAME.propertyName(), inputName);
        	}

        } else {
        	taxaByNameHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
        	if (addSynonyms) {
        		taxaByNameOrSynonymHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
        	}
        }
		
        // create preferred indexes... this is a bit redundant but allows faster searches
        // when the goal is only to search non-hidden taxa (smaller indexes == faster searches)
		if (buildPreferredIndexes && !dubious) {
			prefTaxaByName.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
			prefTaxaByRank.add(tnode, TaxonomyProperty.RANK.propertyName(), rank);

			if (addSynonyms) {
				prefTaxaByNameOrSynonym.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
			}
			
			// add to the rank-specific indexes
	        if (isSpecific(rank)) {
	        	prefTaxaByNameSpecies.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);

	        } else if (rank.equals("genus")) {
	        	prefTaxaByNameGenera.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
	        	prefTaxaByNameHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
	        	if (addSynonyms) {
	        		prefTaxaByNameOrSynonymHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
	        	}

	        } else {
        		prefTaxaByNameHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
	        	if (addSynonyms) {
	        		prefTaxaByNameOrSynonymHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), inputName);
	        	}
	        }
		}
		
		dbNodeForOTTIdMap.put(inputId, tnode);
		
		// if there are no synonyms this map will be empty and will always return null
		ArrayList<String> synNames = ottIdToSynonymNamesMap.get(inputId);
		if (synNames != null) {
			
			for (String synName : synNames) {
				
//				System.out.println("Adding synonym '" + synName + "' for ott id " + String.valueOf(inputId) + " to node " + tnode.getId());
				
				Node synNode = createNode();
				synNode.setProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
				synNode.setProperty("source", sourceName);
				synNode.createRelationshipTo(tnode, TaxonomyRelType.SYNONYMOF);
				
				// indexing
				taxaBySynonym.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
				taxaByNameOrSynonym.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
				synonymNodesBySynonym.add(synNode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
	            if (!isSpecific(rank)) {
	            	taxaByNameOrSynonymHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
					synonymNodesBySynonymHigher.add(synNode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
	            }

	            // additional indexing (smaller indexes) for non-hidden taxa
				if (buildPreferredIndexes && !dubious) {
					prefTaxaBySynonym.add(tnode,OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(),synName);
					prefTaxaByNameOrSynonym.add(tnode,OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(),synName);
					prefSynonymNodesBySynonym.add(synNode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
		            if (!isSpecific(rank)) {
		            	prefTaxaByNameOrSynonymHigher.add(tnode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
						prefSynonymNodesBySynonymHigher.add(synNode, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), synName);
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
		
		deprecatedTaxa = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.DEPRECATED_TAXA);

		// only for synonyms
		if (addSynonyms) {
			taxaBySynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_SYNONYM);
			taxaByNameOrSynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_OR_SYNONYM);
			taxaByNameOrSynonymHigher = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME_OR_SYNONYM_HIGHER);
			synonymNodesBySynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.SYNONYM_NODES_BY_SYNONYM);
			synonymNodesBySynonymHigher = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.SYNONYM_NODES_BY_SYNONYM_HIGHER);
		}

		// only for ott id option
		if (createOTTIdIndexes) {
			taxaByOTTId = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_OTT_ID);
		}

		// only for ott id option
		if (createSourceIdIndexes) {
			taxaBySourceId = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_SOURCE_ID);
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
				prefSynonymNodesBySynonym = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_SYNONYM_NODES_BY_SYNONYM);
				prefSynonymNodesBySynonymHigher = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.PREFERRED_SYNONYM_NODES_BY_SYNONYM_HIGHER);
			}
		}
	}
}

