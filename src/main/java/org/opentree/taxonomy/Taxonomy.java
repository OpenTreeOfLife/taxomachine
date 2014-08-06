package org.opentree.taxonomy;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opentree.exceptions.MultipleHitsException;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.constants.TaxonomyRelType;
import org.opentree.taxonomy.contexts.ContextDescription;
import org.opentree.taxonomy.contexts.ContextNotFoundException;
import org.opentree.taxonomy.contexts.TaxonomyContext;
import org.opentree.taxonomy.contexts.TaxonomyNodeIndex;
import org.opentree.graphdb.GraphDatabaseAgent;

/**
 * This class provides access to methods specific to the taxonomic database itself. Initialization requires a GraphDatabaseAgent object,
 * through which the Taxonomy object will interact with the underlying Neo4J database object.
 * 
 * Most of the interaction with the taxonomy is defined in other class files that make use of or inherit the Taxonomy class
 * (e.g. TaxonomyComparator, TaxonomySynthesizer, TaxonomyLoader, Taxon, etc.). Only the most general methods are defined here.
 * The most frequent use of the Taxonomy class itself should probably be to access objects of of the TaxonomyContext class which
 * are iun turn used to access ALL the node indexes. For more information on node indexes and TaxonomyContext objects, refer
 * to the documentation in the TaxonomyContext class file.
 * 
 * @author cody hinchliff and stephen smith
 * 
 */
public class Taxonomy {

	public GraphDatabaseAgent graphDb;
	public final static String LIFE_NODE_NAME = "life";
	public TaxonomyContext ALLTAXA;
	public Index<Node> taxaByOTTId;
	public Index<Node> taxaByFlag;
	public Index<Node> deprecatedTaxa;

	public static final String[] SPECIFIC_RANKS = {"species", "subspecies", "variety", "varietas", "forma", "form"};

	public Taxonomy(GraphDatabaseAgent gdb) {
		graphDb = gdb;
		initIndexes();
	}
	
	public Taxonomy(GraphDatabaseService gds) {
		graphDb = new GraphDatabaseAgent(gds);
		initIndexes();
	}

	private void initIndexes() {
		ALLTAXA = new TaxonomyContext(ContextDescription.ALLTAXA, this);
		taxaByOTTId = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_OTT_ID);
		deprecatedTaxa = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.DEPRECATED_TAXA);
		taxaByFlag = ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_FLAG);
	}
	
	public Node getLifeNode() {

		Node lifeNode;
		try {
			lifeNode = ALLTAXA.findTaxNodesByName(LIFE_NODE_NAME).get(0);
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Could not get life node");
			lifeNode = null;
		}

		return lifeNode;
	}

	public GraphDatabaseAgent getGraphDb() {
		return graphDb;
	}

	/**
	 * Return a TaxonomyContext object for this taxonomy, as defined by the passed ContextDescription `contextDesc`.
	 * TaxonomyContext objects are used for accessing ALL node indexes. For more information on how to use these objects,
	 * refer to the documentation in the TaxonomyContext class file.
	 * 
	 * @param contextDesc
	 * @return TaxonomyContext
	 */
	public TaxonomyContext getContext(ContextDescription contextDesc) {
		return new TaxonomyContext(contextDesc, this);
	}

	/**
	 * Returns a TaxonomyContext object for the ContextDescription with name matching `name`, ignoring the case of characters.
	 * If no context is found for `name`, returns null. TaxonomyContext objects are used for accessing ALL node indexes. For more
	 * information on how to use these objects, refer to the documentation in the TaxonomyContext class file.
	 * 
	 * @param name
	 * @return TaxonomyContext
	 * @throws ContextNotFoundException
	 */
	public TaxonomyContext getContextByName(String name) throws ContextNotFoundException {

		// if the name is empty or null
		if ((name.trim().length() == 0) || (name == null)) {
			return null;
		}

		// otherwise look for a context
		for (ContextDescription cd : ContextDescription.values()) {
			if (cd.name.equalsIgnoreCase(name)) {
				return this.getContext(cd);
			}
		}

		// if the name doesn't match any existing context
		throw new ContextNotFoundException(name);
	}
	
	/**
	 * Check a rank (provided as a string) against a known set of specific and infraspecific ranks. 
	 * @param rank
	 * @return isSpecific
	 */
	public static boolean isSpecific(String rank) {
		
		boolean isSpecific = false;
		if (rank != null) {
			for (String specificRank : SPECIFIC_RANKS) {
				if (rank.equals(specificRank)) {
					isSpecific = true;
					break;
				}
			}
		}
		
		return isSpecific;
	}

	public Taxon getTaxon(Node node) {
		return new Taxon(node, this);
	}

	/**
	 * Search for a taxon matching the supplied OTT id. If more than one hit is found (bad) throws MultipleHitsException. If no hit is found, returns null.
	 * @return taxNode
	 * @throws MultipleHitsException
	 */
    public Taxon getTaxonForOTTId(final Long ottId) {
        IndexHits<Node> hits = null;
        Node match = null;
        
        try {
        	// first check the standard index
        	hits = taxaByOTTId.get(OTVocabularyPredicate.OT_OTT_ID.propertyName(), ottId);
	        if (hits.size() == 1) {
	        	match = hits.getSingle();
	        } else if (hits.size() > 1) {
	        	throw new MultipleHitsException(ottId);
	        }
	        
	        if (match == null) {
	        	// if we didn't find a taxon, check the deprecated ids
	        	
	        	if (hits != null) {
	        		hits.close();
	        	}
	        	hits = deprecatedTaxa.get(OTVocabularyPredicate.OT_OTT_ID.propertyName(), ottId);
		        if (hits.size() == 1) {
		        	match = hits.getSingle();
		        } else if (hits.size() > 1) {
		        	throw new MultipleHitsException(ottId);
		        }
	        }
        } finally {
        	if (hits != null) {
        		hits.close();
        	}
        }
        
        return match != null ? new Taxon(match, this) : null;
	}
	
	/**
	 * Just get the recognized taxon node that is associated with a given synonym node. Deprecated since the synonym nodes are now only accessible via
	 * traversals that must pass through the associated taxon nodes anyway, but left in case it is becomes useful.
	 * 
	 * @param synonymNode
	 * @return taxNode
	 */
	@Deprecated
	public Node getTaxNodeForSynNode(Node synonymNode) {

		TraversalDescription synonymTraversal = Traversal.description().relationships(TaxonomyRelType.SYNONYMOF, Direction.OUTGOING);

		Node taxNode = null;
		for (Node tn : synonymTraversal.traverse(synonymNode).nodes())
			taxNode = tn;

		return taxNode;
	}

	/**
	 * Finds the directed internodal distance between `n1` and `n2` along relationships of type `relType` by tracing the paths to the LICA
	 * of n1 and n2. Note that this method uses direction to find the LICA, and will not behave properly if the outgoing paths along the
	 * specified relType from n1 and n2 do not intersect.
	 * 
	 * @param n1
	 * @param n2
	 * @param relType
	 * @return distance
	 */
	public int getInternodalDistThroughMRCA(Node n1, Node n2, RelationshipType relType) {

		System.out.println("Node 1: " + n1.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()) + " " + n1.getId() + ", Node 2: " + n2.getProperty(OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName()) + " " + n2.getId());

		TraversalDescription hierarchy = Traversal.description().depthFirst().relationships(relType, Direction.OUTGOING);

		Iterable<Node> firstPath = hierarchy.traverse(n1).nodes();
		Iterable<Node> secondPath = hierarchy.traverse(n2).nodes();

		int i = 0;
		boolean matched = false;
		count: for (Node n : firstPath) {

			int j = 0;
			for (Node m : secondPath) {
				if (n.getId() == m.getId())
					matched = true;
				j++;
			}

			if (matched) {
				i += j;
				break count;
			}

			i++;
		}

		return i;
	}

	/**
	 * provide information about the taxonomy stored in the db
	 * @return
	 */
	public Map<String, Object> getMetadataMap() {
		Node metaNode = this.getLifeNode().getSingleRelationship(TaxonomyRelType.METADATAFOR, Direction.INCOMING).getStartNode();
		Map<String, Object> metadata = new HashMap<String, Object>();
		for (String key : metaNode.getPropertyKeys()) {
			metadata.put(key, metaNode.getProperty(key));
		}
		return metadata;
	}

	/**
	 * Add a metadata entry to this taxonomy. Must be called after the life node has been created, and from within a transaction.
	 * @return
	 */
	public void addMetadataEntry(String key, Object value) {
		Node metaNode;
		try {
			metaNode = this.getLifeNode().getSingleRelationship(TaxonomyRelType.METADATAFOR, Direction.INCOMING).getStartNode();
		} catch (Exception ex) {
			throw new RuntimeException("There was a problem finding the taxonomy root or the metadata node. Is the taxonomy fully installed?");
		}
		metaNode.setProperty(key, value);
	}

	/**
	 * Just a wrapper for the underlying database method defined in the GraphDatabaseAgent class
	 * 
	 * @return Transaction
	 */
	public Node createNode() {
		return graphDb.createNode();
	}

	/**
	 * Just a wrapper for the underlying database method defined in the GraphDatabaseAgent class
	 * 
	 * @return Transaction
	 */
	public Transaction beginTx() {
		return graphDb.beginTx();
	}

	/**
	 * Just a wrapper for the underlying database method defined in the GraphDatabaseAgent class
	 * 
	 * @return Node
	 */
	public Node getNodeById(Long arg0) {
		return graphDb.getNodeById(arg0);
	}
}
