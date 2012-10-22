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
	EmbeddedGraphDatabase graphDb; //was GraphDatabaseService
	protected static Index<Node> taxNodeIndex;
	protected static Index<Relationship> sourceRelIndex;
	protected static Index<Node> prefTaxNodeIndex;
	protected static Index<Node> prefSynNodeIndex;
	protected static Index<Node> synNodeIndex;
	protected static Index<Node> taxSourceIndex;
	
	protected static enum RelTypes implements RelationshipType{
		TAXCHILDOF, //standard rel for tax db, from node to parent
		SYNONYMOF, //relationship for synonyms
		METADATAFOR, //relationship connecting a metadata node to the root of a taxonomy
		PREFTAXCHILDOF//relationship type for preferred relationships
	}
	
	protected static void registerShutdownHook( final EmbeddedGraphDatabase graphDb ){ //was GraphDatabaseService
		Runtime.getRuntime().addShutdownHook( new Thread(){
			@Override
			public void run(){
				graphDb.shutdown();
			}
		});
	}
	
	public void shutdownDB(){
		 registerShutdownHook( graphDb );
	}
	
	public EmbeddedGraphDatabase getGraphDB(){
		return graphDb;
	}
	
	/**
	 * @return Checks taxNodeIndex for `name` and returns null (if the name is not found) or 
	 *  the node using IndexHits<Node>.getSingle()
	 * helper function primarily written to avoid forgetting to call hits.close();
	 */
    Node findTaxNodeByName(final String name) {
        IndexHits<Node> hits = this.taxNodeIndex.get("name", name);
        // Node firstNode = hits.getSingle();
        hits.close();
        return hits;
    }

}
