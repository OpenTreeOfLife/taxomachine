package opentree.tnrs;

import opentree.taxonomy.TaxonomyExplorer;
import org.neo4j.graphdb.Node;

/**
 * @author Cody Hinchliff
 * 
 * Provides methods for performing TNRS queries given a search string according
 * to various options.
 */

public class TNRSQuery {

    public static final TNRSAdapterGNR GNR_ADAPTER = getAdapterGNR();
    
    private String graphName;
    private String searchString;
    private TaxonomyExplorer taxonomy;
    private Node matchedNode;
    
    public TNRSQuery(String _graphName) {
        graphName = _graphName;
        taxonomy = new TaxonomyExplorer(_graphName);
    }
    
    /**
     * @param searchString
     * @param adapters
     * @return results
     * 
     * Performs a TNRS query on 'searchString', and loads the results of the query
     * into a TNRSMatchSet object called 'results', which is returned. 'adapters'
     * is an array of TNRSAdapter objects designating the sources which should be
     * queried. If no adapters are passed, then the query is just performed against
     * the local taxonomy graph.
     */
    public TNRSMatchSet getMatches(String searchString, TNRSAdapter...adapters) {
        TNRSMatchSet results = new TNRSMatchSet(searchString, graphName);
        
        matchedNode = taxonomy.findTaxNodeByName(searchString);
        
        // first: check the graph index (simple)
        if (matchedNode != null) {
            System.out.println("The search query was matched to node " + matchedNode.getId() + ": " + matchedNode.toString());
            results.addDirectMatch(matchedNode);
        }
        
        // second: synonyms (need to interface with stephen)
        // third: whatever adapters are desired. define these within this class and expose them
        // as static instance variables
        
        return results;
    }
    
    public static TNRSAdapterGNR getAdapterGNR() {
        return new TNRSAdapterGNR();
    }    
}
