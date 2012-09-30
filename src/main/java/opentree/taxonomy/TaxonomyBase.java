package opentree.taxonomy;

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
 *  @todo Currently this code is identical to the code base in GraphBase
 */
public abstract class TaxonomyBase {
	EmbeddedGraphDatabase graphDb; //was GraphDatabaseService
	protected static Index<Node> taxNodeIndex;
	protected static Index<Node> graphNodeIndex;
	protected static Index<Relationship> sourceRelIndex;
	
	protected static enum RelTypes implements RelationshipType{
		MRCACHILDOF, //standard rel for graph db, from node to parent
		TAXCHILDOF, //standard rel for tax db, from node to parent
		STREECHILDOF, //standard rel for input tree, from node to parent  
		ISCALLED // is called ,from node in graph of life to node in tax graph 
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
	 * @return Checks graphNodeIndex for `name` and returns null (if the name is not found) or 
	 * the node using IndexHits<Node>.getSingle()
	 * helper function primarily written to avoid forgetting to call hits.close();
	 * 
	 * What is the 
	 */
    Node findGraphNodeByName(final String name) {
        IndexHits<Node> hits = this.graphNodeIndex.get("name", name);
		Node firstNode = hits.getSingle();
		hits.close();
		return firstNode;
	}
    
    /**
     * @return Checks taxNodeIndex for `name` and returns an empty hit list (if the name is not found)
     * Uses fuzzy searching.
     */
    public IndexHits<Node> findTaxNodeByNameFuzzy(final String name) {
        IndexHits<Node> hits = this.taxNodeIndex.query(new FuzzyQuery(new Term("name", name)));
        hits.close();
        return hits;
    }
    
	/**
	 * @return Checks taxNodeIndex for `name` and returns null (if the name is not found) or 
	 * the node using IndexHits<Node>.getSingle()
	 * helper function primarily written to avoid forgetting to call hits.close();
	 *  
	 * the benefit of doing this might be outweighed by the difficulty of dealing with all
	 * potential macthes here, rather than having access to them all elsewhere. Not a problem
	 * if we can return hits after it has been closed, but that seems like weird behavior.
	 */
    public IndexHits<Node> findTaxNodeByName(final String name) {
        IndexHits<Node> hits = this.taxNodeIndex.get("name", name);
//		Node firstNode = hits.getSingle();
		hits.close();
		return hits;
	}

}
