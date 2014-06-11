package org.opentree.taxonomy.contexts;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.opentree.properties.OTVocabularyPredicate;
import org.opentree.taxonomy.Taxonomy;
import org.opentree.taxonomy.contexts.ContextDescription;
import org.apache.lucene.queryParser.QueryParser;

/**
 * TaxonomyContext objects are used to access ALL node indexes. The various taxonomic contexts are defined in the ContextDescription enum. To initialize a
 * TaxonomyContext object, a ContextDescription object `cd` and a Taxonomy object `t` must be passed to the TaxonomyContext constructor. The returned
 * TaxonomyContext object will provide access to node indexes of `t` which will be limited to the taxonomic scope defined by `cd`. The most straightforward way
 * to create a new TaxonomyContext object is simply to pass a ContextDescription object to the Taxonomy.getContext() method, which will return a TaxonomyContext
 * object for the originating Taxonomy and indicated ContextDescription.
 * 
 * Before TaxonomyContext objects can be used to query the indexes, the indexes themselves must first be built using the TaxonomySynthesizer.makeContexts()
 * method. For more information on this, refer to the documentation for that method in the TaxonomySynthesizer class file.
 * 
 * Once the indexes have been built, it becomes possible to query the node indexes by name, synonym, or various other properties. The types of indexes that will
 * be built (and thus the types of available queries) are named and discussed in the NodeIndexDescription class file, with more specific details in the various
 * methods of the TaxonomySynthesizer class file that are called by the TaxonomySynthesizer.makeContexts() method itself.
 * 
 * Once a TaxonomyContext object `tc` has been created, its node indexes themselves can be accessed through the tc.getNodeIndex(NodeIndexDescription nid)
 * method. This will call the underlying Neo4J graph database (via the GraphDatabaseAgent that underlies the associated Taxonomy object) and will return
 * a Neo4J Index<Node> object that can be queried using any available methods.
 * 
 * Various convenience wrappers are defined to simplify certain common queries, such as findTaxNodesByName(String name), which returns results from a query to
 * the TaxNodesByName index. These methods also provide examples illustrating how a node index may be queried from a ContextDescription object.
 * 
 * To access the non-context specific node indexes (i.e. the entire taxonomy at once), initialize a TaxonomyContext object using the ContextDescription.ALLTAXA
 * enum. Assuming that a GraphDatabaseAgent object `gdb` exists that points to a taxomachine database, the code to create a context and get a node index by name
 * might look something like this:
 * 
 * <pre>
 * Taxonomy t = new Taxonomy(gdb);
 * TaxonomyContext everything = t.getContext(ContextDescription.ALLTAXA);
 * Index<Node> allTaxaIndexByName = (Index<Node>) everything.getNodeIndex(NodeIndexDescription.TAXON_BY_NAME);
 * </pre>
 * 
 * @author cody hinchliff
 * 
 */

public class TaxonomyContext {

	private ContextDescription contextDescription;
	private Taxonomy taxonomy;

	public TaxonomyContext(ContextDescription context, Taxonomy taxonomy) {
		this.contextDescription = context;
		this.taxonomy = taxonomy;

	}

	/**
	 * Return a Neo4J index of the type defined by the passed NodeIndexDescription `index`, which is intended to be limited to the scope of the initiating
	 * taxonomic context
	 * 
	 * @param indexDesc
	 * @return nodeIndex
	 */
	public Index<Node> getNodeIndex(TaxonomyNodeIndex indexDesc) {
		String indexName = indexDesc.namePrefix + contextDescription.nameSuffix;
		return taxonomy.getGraphDb().getNodeIndex(indexName, "type", "exact", "to_lower_case", "true");

	}

	/**
	 * Return the ContextDescription that underlies this TaxonomyContext object.
	 * 
	 * @return
	 */
	public ContextDescription getDescription() {
		return contextDescription;
	}

	/**
	 * Return the root node for this taxonomic context
	 * 
	 * @return rootNode
	 */
	public Node getRootNode() {
		IndexHits<Node> rootMatches = taxonomy.ALLTAXA.getNodeIndex(TaxonomyNodeIndex.TAXON_BY_OTT_ID)
				.get(OTVocabularyPredicate.OT_OTT_ID.propertyName(), contextDescription.ottId);
		try {
			try {
				return rootMatches.getSingle();
			} catch (Exception ex) {
				throw new java.lang.IllegalStateException("There was a problem getting the root node: " + contextDescription.licaNodeName + " for context "
						+ contextDescription.name);
			}
		} finally {
			rootMatches.close();
		}
	}

	/**
	 * a convenience wrapper
	 * 
	 * @param name
	 * @return
	 */
	public List<Node> findTaxNodesByName(String name) {
		Index<Node> index = (Index<Node>) getNodeIndex(TaxonomyNodeIndex.TAXON_BY_NAME);
		return findNodes(index, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), name);
	}

	/**
	 * a convenience wrapper
	 * 
	 * @param name
	 * @return
	 */
	public List<Node> findPrefTaxNodesByName(String name) {
		Index<Node> index = (Index<Node>) getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME);
		return findNodes(index, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), name);

	}

	/**
	 * a generic wrapper for node indexes that searches the specified index for entries with `column` matching `key`
	 * 
	 * have to escape a bunch of characters that hiccup on taxon searches
	 * 
	 * @param index
	 * @param column
	 * @param key
	 * @return
	 */
	public List<Node> findNodes(Index<Node> index, String column, String key) {

		ArrayList<Node> foundNodes = new ArrayList<Node>();
		// lucene index.query() method will split search terms on spaces and use only the first; we must escape spaces to avoid this
		key = QueryParser.escape(key).replace(" ", "\\ ");
		IndexHits<Node> results = index.query(column, key);
		for (Node n : results) {
			foundNodes.add(n);
		}
		results.close();

		return foundNodes;
	}

	public List<Node> findPrefTaxNodesByNameOrSyn(String name) {
		Index<Node> index = (Index<Node>) getNodeIndex(TaxonomyNodeIndex.PREFERRED_TAXON_BY_NAME_OR_SYNONYM);
		return findNodes(index, OTVocabularyPredicate.OT_OTT_TAXON_NAME.propertyName(), name);

	}

}
