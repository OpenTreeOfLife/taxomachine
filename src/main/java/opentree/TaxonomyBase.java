package opentree;

import java.util.ArrayList;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * @todo Currently this code is identical to the code base in GraphBase
 */
public abstract class TaxonomyBase {
	EmbeddedGraphDatabase graphDb; // was GraphDatabaseService
	protected static Index<Node> taxNodeIndex;
	protected static Index<Relationship> sourceRelIndex;
	protected static Index<Node> prefTaxNodeIndex;
	protected static Index<Node> prefSynNodeIndex;
	protected static Index<Node> synNodeIndex;
	protected static Index<Node> taxSourceIndex;
	final static float DEFAULT_FUZZYMATCH_ID = (float)0.70;

	protected static enum RelTypes implements RelationshipType {
		TAXCHILDOF, // standard rel for tax db, from node to parent
		SYNONYMOF, // relationship for synonyms
		METADATAFOR, // relationship connecting a metadata node to the root of a taxonomy
		PREFTAXCHILDOF// relationship type for preferred relationships
	}

	protected static void registerShutdownHook(
			final EmbeddedGraphDatabase graphDb) { // was GraphDatabaseService
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

	public void shutdownDB() {
		registerShutdownHook(graphDb);
	}

	public EmbeddedGraphDatabase getGraphDB() {
		return graphDb;
	}

	/**
	 * Checks taxNodeIndex for `name` and returns null (if the name is not found) or an IndexHits<Node> object containing the found nodes. Helper function
	 * primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits` after it has been closed, but it works. Would be
	 * helpful to validate that this is expected behavior.
	 * 
	 * @return
	 */
	public IndexHits<Node> findTaxNodeByName(final String name) {
		IndexHits<Node> hits = taxNodeIndex.get("name", name); // TODO: change to preferred nodes when this is functional
		hits.close();
		return hits;
	}

	/**
	 * Checks synNodeIndex for `name` and returns null (if the name is not found) or an IndexHits<Node> object containing the found nodes. Helper function
	 * primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits` after it has been closed, but it works. Would be
	 * helpful to validate that this is expected behavior.
	 * 
	 * @return
	 */
	public IndexHits<Node> findSynNodeByName(final String name) {
		IndexHits<Node> hits = synNodeIndex.get("name", name); // TODO: change to preferred nodes when this is functional
		hits.close();
		return hits;
	}

	/**
	 * Checks taxNodeIndex for `name` and returns all hits. Uses fuzzy searching. Returns null (if the name is not found) or an IndexHits<Node> object
	 * containing the found nodes. Helper function primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits`
	 * after it has been closed, but it works. Would be helpful to validate that this is expected behavior.
	 * 
	 * @return
	 */
	public IndexHits<Node> findTaxNodeByNameFuzzy(final String name, float minIdentity) {
		IndexHits<Node> hits = taxNodeIndex.query(new FuzzyQuery(new Term("name", name), minIdentity)); // TODO: change to preferred nodes when this is functional
		hits.close();
		return hits;
	}

	/**
	 * A wrapper that uses the default minimum identity score for fuzzy matches
	 * @param name
	 * @return
	 */
	public IndexHits<Node> findTaxNodeByNameFuzzy(final String name) {
		return findTaxNodeByNameFuzzy(name, DEFAULT_FUZZYMATCH_ID);
	}
	
	/**
	 * Checks synNodeIndex for `name` and returns all hits. Uses fuzzy searching. Returns null (if the name is not found) or an IndexHits<Node> object
	 * containing the found nodes. Helper function primarily written to avoid forgetting to call hits.close(). Somewhat disconcerting that we return `hits`
	 * after it has been closed, but it works. Would be helpful to validate that this is expected behavior.
	 * 
	 * @return
	 */
	public IndexHits<Node> findSynNodeByNameFuzzy(final String name, float minIdentity) {
		IndexHits<Node> hits = synNodeIndex.query(new FuzzyQuery(new Term("name", name), minIdentity)); // TODO: change to preferred nodes when this is functional
		hits.close();
		return hits;
	}
	
	/**
	 * A wrapper that uses the default minimum identity score for fuzzy matches
	 * @param name
	 * @return
	 */
	public IndexHits<Node> findSynNodeByNameFuzzy(final String name) {
		return findSynNodeByNameFuzzy(name, DEFAULT_FUZZYMATCH_ID);
	}
	
}
